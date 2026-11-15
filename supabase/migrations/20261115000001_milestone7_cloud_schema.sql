-- SOLO LEVELING — REALITY OS (Milestone 7)
-- Normalized PostgreSQL Cloud Schema for Supabase
-- Supports multi-device UUID stability, incremental sync cursors, tombstones,
-- provenance preservation, and immutable progression audit history.

create extension if not exists "pgcrypto";

-- Generic updated_at trigger
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

-- 1. Player Identity & Authoritative Progression Snapshot (Protected columns)
create table if not exists public.player_identities (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique references auth.users(id) on delete cascade,
    display_name text not null check (char_length(display_name) between 2 and 32),
    active_title text not null default 'Awakened E-Rank',
    account_status text not null default 'ACTIVE' check (account_status in ('ACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED', 'DELETED')),
    -- Authoritative progression fields (client cannot mutate directly; updated by Edge Function)
    level integer not null default 1 check (level >= 1),
    current_level_xp integer not null default 0 check (current_level_xp >= 0),
    total_xp bigint not null default 0 check (total_xp >= 0),
    rank_code text not null default 'E_RANK',
    str_verified integer not null default 0,
    str_self_reported integer not null default 0,
    str_progress_xp integer not null default 0,
    vit_verified integer not null default 0,
    vit_self_reported integer not null default 0,
    vit_progress_xp integer not null default 0,
    agi_verified integer not null default 0,
    agi_self_reported integer not null default 0,
    agi_progress_xp integer not null default 0,
    int_verified integer not null default 0,
    int_self_reported integer not null default 0,
    int_progress_xp integer not null default 0,
    dis_verified integer not null default 0,
    dis_self_reported integer not null default 0,
    dis_progress_xp integer not null default 0,
    wis_verified integer not null default 0,
    wis_self_reported integer not null default 0,
    wis_progress_xp integer not null default 0,
    cha_verified integer not null default 0,
    cha_self_reported integer not null default 0,
    cha_progress_xp integer not null default 0,
    streak_days integer not null default 0,
    verified_activities_count integer not null default 0,
    self_reported_activities_count integer not null default 0,
    recovery_score integer not null default 100,
    last_sync_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_player_identities_user_updated on public.player_identities(user_id, updated_at);
drop trigger if exists trg_player_identities_updated on public.player_identities;
create trigger trg_player_identities_updated before update on public.player_identities for each row execute function public.set_updated_at();

-- 2. User-Editable Settings & Consent Preferences
create table if not exists public.player_settings (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique references auth.users(id) on delete cascade,
    hud_glow_enabled boolean not null default true,
    daily_reset_hour integer not null default 4 check (daily_reset_hour between 0 and 23),
    health_connect_sync_enabled boolean not null default false,
    supabase_cloud_sync_enabled boolean not null default true,
    academic_daily_goal_minutes integer not null default 60,
    academic_weekly_goal_minutes integer not null default 360,
    ai_model_id text not null default 'google/gemini-2.5-flash',
    cloud_ai_consent_granted boolean not null default false,
    notification_prefs_json jsonb not null default '{}'::jsonb,
    version integer not null default 1,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_player_settings_user_updated on public.player_settings(user_id, updated_at);
drop trigger if exists trg_player_settings_updated on public.player_settings;
create trigger trg_player_settings_updated before update on public.player_settings for each row execute function public.set_updated_at();

-- 3. Multi-Device Registrations & Sync Cursors
create table if not exists public.player_devices (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    device_name text not null,
    manufacturer_model text not null,
    os_version text not null,
    app_version text not null,
    sync_status text not null default 'SYNCED',
    is_revoked boolean not null default false,
    last_active_at timestamptz not null default now(),
    last_sync_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_player_devices_user_active on public.player_devices(user_id, last_active_at desc);
drop trigger if exists trg_player_devices_updated on public.player_devices;
create trigger trg_player_devices_updated before update on public.player_devices for each row execute function public.set_updated_at();

create table if not exists public.sync_metadata (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    device_id uuid not null references public.player_devices(id) on delete cascade,
    entity_type text not null,
    last_cursor_ts timestamptz not null default '1970-01-01T00:00:00Z',
    records_synced_count integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, device_id, entity_type)
);

-- 4. Physical Development Records (Provenance-preserved)
create table if not exists public.physical_records (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    source_id text not null,
    record_type text not null check (record_type in ('JOGGING', 'WORKOUT', 'RECOVERY')),
    title text not null,
    activity_kind text not null,
    origin_app text not null,
    verification_status text not null,
    start_epoch_ms bigint not null,
    end_epoch_ms bigint not null,
    duration_minutes integer not null,
    distance_meters double precision,
    avg_hr_bpm integer,
    readiness_score integer,
    payload_json jsonb not null default '{}'::jsonb,
    xp_awarded integer not null default 0,
    version integer not null default 1,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, source_id)
);
create index if not exists idx_physical_records_user_updated on public.physical_records(user_id, updated_at);

-- 5. Academic Development Records
create table if not exists public.academic_subjects (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    code text not null,
    program_system text not null,
    priority text not null,
    is_archived boolean not null default false,
    payload_json jsonb not null default '{}'::jsonb,
    version integer not null default 1,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_academic_subjects_user_updated on public.academic_subjects(user_id, updated_at);

create table if not exists public.academic_records (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    subject_uuid uuid references public.academic_subjects(id) on delete set null,
    source_id text not null,
    record_type text not null check (record_type in ('STUDY_SESSION', 'REVISION', 'ASSESSMENT', 'EXAM', 'ACADEMIC_QUEST')),
    title text not null,
    verification_status text not null,
    xp_awarded integer not null default 0,
    payload_json jsonb not null default '{}'::jsonb,
    version integer not null default 1,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, source_id)
);
create index if not exists idx_academic_records_user_updated on public.academic_records(user_id, updated_at);

-- 6. Life Management: Routines, Habits, Personal Tasks & Goals
create table if not exists public.life_routines (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    time_slot text not null,
    start_minute_of_day integer not null,
    is_enabled boolean not null default true,
    payload_json jsonb not null default '{}'::jsonb,
    version integer not null default 1,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_life_routines_user_updated on public.life_routines(user_id, updated_at);

create table if not exists public.habits (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    habit_type text not null,
    category text not null,
    target_value double precision not null,
    unit text not null,
    is_archived boolean not null default false,
    payload_json jsonb not null default '{}'::jsonb,
    version integer not null default 1,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_habits_user_updated on public.habits(user_id, updated_at);

create table if not exists public.habit_completions (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    habit_id uuid not null references public.habits(id) on delete cascade,
    date_epoch_day bigint not null,
    status text not null,
    achieved_value double precision not null default 0,
    note text not null default '',
    recorded_at_epoch_ms bigint not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, habit_id, date_epoch_day)
);
create index if not exists idx_habit_completions_user_updated on public.habit_completions(user_id, updated_at);

create table if not exists public.routine_completions (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    routine_id uuid not null references public.life_routines(id) on delete cascade,
    activity_key text not null,
    date_epoch_day bigint not null,
    status text not null,
    completed_at_epoch_ms bigint not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, routine_id, activity_key, date_epoch_day)
);
create index if not exists idx_routine_completions_user_updated on public.routine_completions(user_id, updated_at);

create table if not exists public.personal_tasks (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    description text not null default '',
    category text not null,
    priority text not null,
    status text not null,
    due_date_epoch_day bigint,
    estimated_minutes integer not null default 30,
    linked_goal_id uuid,
    postpone_count integer not null default 0,
    completed_at_epoch_ms bigint,
    payload_json jsonb not null default '{}'::jsonb,
    version integer not null default 1,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_personal_tasks_user_updated on public.personal_tasks(user_id, updated_at);

create table if not exists public.life_goals (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    timeframe text not null,
    status text not null,
    target_attribute text not null,
    payload_json jsonb not null default '{}'::jsonb,
    version integer not null default 1,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_life_goals_user_updated on public.life_goals(user_id, updated_at);

-- 7. RPG System State (Dungeons, Boss Battles, Skills, Achievements, Titles, Inventory, Shadows)
create table if not exists public.rpg_progress_records (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    record_kind text not null check (record_kind in ('DUNGEON_INSTANCE', 'BOSS_INSTANCE', 'SKILL_UNLOCK', 'ACHIEVEMENT_UNLOCK', 'TITLE_STATE', 'INVENTORY_ITEM', 'SHADOW_COLLECTIBLE')),
    stable_code text not null,
    status text not null,
    payload_json jsonb not null default '{}'::jsonb,
    version integer not null default 1,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, record_kind, stable_code)
);
create index if not exists idx_rpg_progress_user_updated on public.rpg_progress_records(user_id, updated_at);

-- 8. Immutable Progression & Reward Audit Ledger (Write-only via Trusted Edge Function)
create table if not exists public.progression_events (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    idempotency_key text not null,
    source_module text not null,
    source_record_id text not null,
    device_id uuid,
    verification_status text not null,
    xp_awarded integer not null check (xp_awarded >= 0),
    primary_attribute text not null,
    primary_attr_xp integer not null check (primary_attr_xp >= 0),
    secondary_attribute text,
    secondary_attr_xp integer not null default 0 check (secondary_attr_xp >= 0),
    was_capped boolean not null default false,
    validation_note text not null default '',
    occurred_at timestamptz not null,
    created_at timestamptz not null default now(),
    unique (user_id, idempotency_key)
);
create index if not exists idx_progression_events_user_created on public.progression_events(user_id, created_at desc);

create table if not exists public.rpg_reward_audit (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    reward_key text not null,
    source_type text not null,
    source_id text not null,
    title text not null,
    xp_awarded integer not null check (xp_awarded >= 0),
    primary_attr_name text not null,
    primary_attr_xp integer not null check (primary_attr_xp >= 0),
    secondary_attr_name text,
    secondary_attr_xp integer not null default 0 check (secondary_attr_xp >= 0),
    verification_provenance text not null,
    was_capped boolean not null default false,
    notes text not null default '',
    recorded_at timestamptz not null,
    created_at timestamptz not null default now(),
    unique (user_id, reward_key)
);
create index if not exists idx_rpg_reward_audit_user_created on public.rpg_reward_audit(user_id, created_at desc);

-- 9. Cloud Backups Metadata & Snapshot Archive
create table if not exists public.cloud_backups (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    device_id uuid references public.player_devices(id) on delete set null,
    schema_version integer not null,
    progression_checksum text not null,
    total_xp_snapshot bigint not null,
    player_level_snapshot integer not null,
    record_count integer not null,
    summary_text text not null,
    snapshot_json jsonb not null,
    status text not null default 'UPLOADED' check (status in ('UPLOADED', 'VERIFIED', 'RESTORED', 'FAILED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_cloud_backups_user_created on public.cloud_backups(user_id, created_at desc);
