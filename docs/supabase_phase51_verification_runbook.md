# Supabase Phase 5.1 / 5.1.1 RLS & Integrity Verification Runbook

This runbook provides reproducible verification steps for live or local Supabase Postgres instances. None of the verification queries below modify persisted production state (`supabase/tests/phase51_rls_verification.sql` executes strictly inside `BEGIN ... ROLLBACK;`).

## 1. Prerequisites & Safe Migration Order

Apply migrations forward-only in exact chronological order via Supabase CLI (`supabase db push` or `psql`):

1. `supabase/migrations/20261115000001_milestone7_cloud_schema.sql`
2. `supabase/migrations/20261117000001_milestone12_social_mastery.sql`
3. `supabase/migrations/20261118000001_milestone12_phase51_rls_hardening.sql`
4. `supabase/migrations/20261118000002_milestone12_phase511_integrity_hotfix.sql`

Deploy the hardened progression Edge Function (`SOCIAL_MASTERY` `180 XP/day` & `3 self-reported/day` cap):

```bash
supabase functions deploy progression-validate
```

## 2. Option A — Automated Rollback-Safe Postgres SQL Test

Run against a local Supabase CLI Postgres database (`supabase start`) or staging database URI using a role permitted to assume `authenticated` / `anon` within a rolled-back transaction:

```bash
psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 -f supabase/tests/phase51_rls_verification.sql
```

Verified assertions inside `phase51_rls_verification.sql`:
- **SELECT isolation**: User B receives `0` rows across all 10 `public.social_*` tables while User A rows exist.
- **INSERT ownership**: User B spoofing `user_id = user_a` fails with `insufficient_privilege` (`42501`).
- **Parent-plan ownership (INSERT)**: User B inserting into `social_planned_activities` referencing User A's `plan_id` fails.
- **Parent-plan ownership (UPDATE)**: User A updating their activity's `plan_id` to User B's `plan_id` fails.
- **UPDATE / DELETE isolation**: User B updates/deletes targeting User A rows affect `0` rows.
- **Immutable `user_id` trigger**: Even owner User A receives `Ownership transfer denied` when attempting `UPDATE ... SET user_id = user_b`.
- **Anonymous denial**: `anon` role with empty `request.jwt.claim.sub` reads `0` rows and fails `INSERT`.
- **Transaction cleanup**: Ends cleanly with `ROLLBACK`.

## 3. Option B — Two Live Authenticated Test Users (PostgREST / API Level)

Use two disposable test accounts (`USER_A_JWT`, `USER_B_JWT` from Supabase Auth `/auth/v1/token?grant_type=password`) against `$SUPABASE_URL/rest/v1/`:

1. **Seed User A plan & competency (as User A)**:
   - `POST /rest/v1/social_weekly_training_plans` with `Authorization: Bearer $USER_A_JWT` -> HTTP `201`.
2. **Verify User B SELECT isolation**:
   - `GET /rest/v1/social_weekly_training_plans?user_id=eq.$USER_A_ID` with `Authorization: Bearer $USER_B_JWT` -> `200 []` (empty array).
3. **Verify User B spoofed INSERT rejected**:
   - `POST /rest/v1/social_profiles` with `{"user_id":"$USER_A_ID","last_active_epoch_ms":1700000000000}` using `$USER_B_JWT` -> HTTP `403` / `42501`.
4. **Verify User B cross-user parent-plan hijack rejected**:
   - `POST /rest/v1/social_planned_activities` with `user_id = $USER_B_ID` and `plan_id = $USER_A_PLAN_ID` using `$USER_B_JWT` -> HTTP `403` / trigger error.
5. **Verify User B UPDATE / DELETE on User A records affects 0 rows**:
   - `PATCH /rest/v1/social_adaptive_recommendations?recommendation_id=eq.$REC_A_ID` using `$USER_B_JWT` with `Prefer: return=representation` -> `200 []`.
   - `DELETE /rest/v1/social_weekly_training_plans?plan_id=eq.$PLAN_A_ID` using `$USER_B_JWT` with `Prefer: return=representation` -> `200 []`.
6. **Verify owner `user_id` immutability**:
   - `PATCH /rest/v1/social_competency_progress?competency_id=eq.$COMP_A_ID` with `{"user_id":"$USER_B_ID"}` using `$USER_A_JWT` -> error `Ownership transfer denied`.
7. **Cleanup test users** after verification via Supabase Auth Admin API or test project reset.
