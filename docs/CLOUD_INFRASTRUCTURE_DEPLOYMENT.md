# SOLO LEVELING — REALITY OS: Cloud Infrastructure & Synchronization Deployment Guide

## 1. Security Architecture Summary
- **Client Keys Only in Android APK:** The Android app only reads `SUPABASE_URL` and `SUPABASE_ANON_KEY` via AI Studio Secrets / `.env` → `BuildConfig`.
- **Zero Privileged Secrets on Client:** `SUPABASE_SERVICE_ROLE_KEY` and `OPENROUTER_API_KEY` live **exclusively** inside Supabase Edge Function server secrets.
- **Row Level Security (RLS):** Every table in `public` enforces `auth.uid() = user_id`. Direct client mutation of authoritative XP, Level, Hunter Rank, Attributes, `progression_events`, and `rpg_reward_audit` is blocked by RLS + PostgreSQL trigger `prevent_client_progression_tamper()`.

## 2. Supabase Project Setup
1. Create a Supabase project at `https://supabase.com`.
2. Link locally using Supabase CLI:
   ```bash
   supabase login
   supabase link --project-ref <YOUR_PROJECT_REF>
   ```
3. Push database schema and RLS migrations:
   ```bash
   supabase db push
   ```
   This applies:
   - `supabase/migrations/20261115000001_milestone7_cloud_schema.sql`
   - `supabase/migrations/20261115000002_milestone7_rls_and_security.sql`

## 3. Configure Supabase Auth
In **Supabase Dashboard → Authentication → Providers & Settings**:
- Enable **Email** provider with **Confirm email** enabled.
- Set Redirect URLs for email verification and password reset if using deep links:
  - `sololeveling://auth/callback`
  - `sololeveling://auth/reset-password`
- Future OAuth providers (e.g., Google / GitHub) can be enabled here and connected cleanly via `SupabaseAuthPort.buildOAuthAuthorizeUrl(provider)`.

## 4. Deploy Edge Functions
Configure server-side secrets:
```bash
supabase secrets set OPENROUTER_API_KEY="sk-or-v1-..."
# SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are automatically injected in Supabase Edge Functions
```
Deploy all four Edge Functions:
```bash
supabase functions deploy ai-game-master --no-verify-jwt
supabase functions deploy progression-validate
supabase functions deploy account-delete
supabase functions deploy backup-restore
```

## 5. Android Environment Configuration
Open the **Secrets panel in AI Studio** (do NOT put secrets in `local.properties` or commit `.env` to Git):
- `SUPABASE_URL`: `https://<YOUR_PROJECT_REF>.supabase.co`
- `SUPABASE_ANON_KEY`: `<YOUR_SUPABASE_PUBLISHABLE_ANON_KEY>`

For local development builds without live cloud credentials, the app operates 100% offline-first using local Room persistence, outbox queuing (`sync_queue`), and conflict/backup local previews.

## 6. Verification Checklist
- **Local Automated Suite:** `gradle :app:testDebugUnitTest --console=plain`
- **APK Build:** `compile_applet`
- **Live Cloud Integration (post-deployment by operator):** Sign up two test users, confirm RLS blocks cross-user reads on `physical_records`, `habits`, and `progression_events`, and confirm `player_identities.total_xp` cannot be mutated via direct PostgREST `PATCH`.
