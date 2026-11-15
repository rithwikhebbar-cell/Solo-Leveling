-- SOLO LEVELING — REALITY OS (Milestone 7)
-- Row Level Security (RLS) & Anti-Tampering Guards
-- Enforces strict auth.uid() ownership across all tables and blocks direct client
-- manipulation of XP totals, Hunter Ranks, Attributes, and Immutable Audit Logs.

-- Enable RLS on every user table
alter table public.player_identities enable row level security;
alter table public.player_settings enable row level security;
alter table public.player_devices enable row level security;
alter table public.sync_metadata enable row level security;
alter table public.physical_records enable row level security;
alter table public.academic_subjects enable row level security;
alter table public.academic_records enable row level security;
alter table public.life_routines enable row level security;
alter table public.habits enable row level security;
alter table public.habit_completions enable row level security;
alter table public.routine_completions enable row level security;
alter table public.personal_tasks enable row level security;
alter table public.life_goals enable row level security;
alter table public.rpg_progress_records enable row level security;
alter table public.progression_events enable row level security;
alter table public.rpg_reward_audit enable row level security;
alter table public.cloud_backups enable row level security;

-- 1. Player Identities: Users may read & initialize display name ONLY; progression fields are server-managed
create policy "player_identities_select_own" on public.player_identities
    for select using (auth.uid() = user_id);

create policy "player_identities_insert_own_initial" on public.player_identities
    for insert with check (
        auth.uid() = user_id
        and level = 1
        and total_xp = 0
        and current_level_xp = 0
        and rank_code = 'E_RANK'
    );

create policy "player_identities_update_display_only" on public.player_identities
    for update using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

-- Trigger blocking authenticated client roles from tampering with protected progression fields
create or replace function public.prevent_client_progression_tamper()
returns trigger
language plpgsql
as $$
begin
    if current_setting('request.jwt.claim.role', true) <> 'service_role' then
        if new.level is distinct from old.level
           or new.current_level_xp is distinct from old.current_level_xp
           or new.total_xp is distinct from old.total_xp
           or new.rank_code is distinct from old.rank_code
           or new.str_verified is distinct from old.str_verified
           or new.vit_verified is distinct from old.vit_verified
           or new.agi_verified is distinct from old.agi_verified
           or new.int_verified is distinct from old.int_verified
           or new.dis_verified is distinct from old.dis_verified
           or new.wis_verified is distinct from old.wis_verified
           or new.cha_verified is distinct from old.cha_verified
           or new.verified_activities_count is distinct from old.verified_activities_count then
            raise exception 'Progression fields cannot be modified directly by client. Use progression-validate.';
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists trg_prevent_progression_tamper on public.player_identities;
create trigger trg_prevent_progression_tamper
    before update on public.player_identities
    for each row execute function public.prevent_client_progression_tamper();

-- 2. User-owned mutable tables (strict auth.uid() = user_id)
create policy "player_settings_all_own" on public.player_settings
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "player_devices_all_own" on public.player_devices
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "sync_metadata_all_own" on public.sync_metadata
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "physical_records_all_own" on public.physical_records
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "academic_subjects_all_own" on public.academic_subjects
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "academic_records_all_own" on public.academic_records
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "life_routines_all_own" on public.life_routines
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "habits_all_own" on public.habits
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "habit_completions_all_own" on public.habit_completions
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "routine_completions_all_own" on public.routine_completions
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "personal_tasks_all_own" on public.personal_tasks
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "life_goals_all_own" on public.life_goals
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "rpg_progress_all_own" on public.rpg_progress_records
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "cloud_backups_select_insert_own" on public.cloud_backups
    for select using (auth.uid() = user_id);

create policy "cloud_backups_insert_own" on public.cloud_backups
    for insert with check (auth.uid() = user_id);

-- 3. Immutable Audit & Progression Tables: READ-ONLY for client; WRITE-ONLY via Edge Function service_role
create policy "progression_events_select_own" on public.progression_events
    for select using (auth.uid() = user_id);

create policy "rpg_reward_audit_select_own" on public.rpg_reward_audit
    for select using (auth.uid() = user_id);
-- No INSERT/UPDATE/DELETE policies are defined on progression_events or rpg_reward_audit for authenticated users.
