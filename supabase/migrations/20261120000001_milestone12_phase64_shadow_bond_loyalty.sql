-- ============================================================================
-- SOLO LEVELING — REALITY OS
-- MILESTONE 12 — PHASE 6.4: SHADOW BOND & LOYALTY SYSTEM
-- Target tables:
--   1. public.shadow_bond_profiles
--   2. public.shadow_bond_events
--   3. public.shadow_bond_milestones
-- ============================================================================

-- 1. Create shadow_bond_profiles table (mirrors Room v21 shadow_bond_profiles)
CREATE TABLE IF NOT EXISTS public.shadow_bond_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    shadow_id TEXT NOT NULL,
    bond_level INTEGER NOT NULL DEFAULT 1,
    bond_points BIGINT NOT NULL DEFAULT 0,
    loyalty_rating INTEGER NOT NULL DEFAULT 15,
    trust_rating INTEGER NOT NULL DEFAULT 10,
    relationship_stage TEXT NOT NULL DEFAULT 'UNFAMILIAR',
    personality TEXT NOT NULL DEFAULT 'DISCIPLINED',
    total_activities_count INTEGER NOT NULL DEFAULT 0,
    last_bond_activity_epoch_ms BIGINT NOT NULL,
    updated_at_epoch_ms BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_bond_profile_per_user UNIQUE (user_id, shadow_id)
);

CREATE INDEX IF NOT EXISTS idx_shadow_bond_profiles_user_shadow 
    ON public.shadow_bond_profiles(user_id, shadow_id);
CREATE INDEX IF NOT EXISTS idx_shadow_bond_profiles_stage 
    ON public.shadow_bond_profiles(user_id, relationship_stage);

-- 2. Create shadow_bond_events table (mirrors Room v21 shadow_bond_events)
CREATE TABLE IF NOT EXISTS public.shadow_bond_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    event_id TEXT NOT NULL,
    shadow_id TEXT NOT NULL,
    activity_type TEXT NOT NULL,
    source_event_id TEXT NOT NULL,
    bond_points_awarded INTEGER NOT NULL,
    loyalty_delta INTEGER NOT NULL,
    trust_delta INTEGER NOT NULL,
    description TEXT NOT NULL,
    timestamp_epoch_ms BIGINT NOT NULL,
    idempotency_key TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_bond_event_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_shadow_bond_events_user_shadow_time 
    ON public.shadow_bond_events(user_id, shadow_id, timestamp_epoch_ms);
CREATE INDEX IF NOT EXISTS idx_shadow_bond_events_idempotency 
    ON public.shadow_bond_events(user_id, idempotency_key);

-- 3. Create shadow_bond_milestones table (mirrors Room v21 shadow_bond_milestones)
CREATE TABLE IF NOT EXISTS public.shadow_bond_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    milestone_id TEXT NOT NULL,
    shadow_id TEXT NOT NULL,
    milestone_type TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    source_event_id TEXT NOT NULL,
    timestamp_epoch_ms BIGINT NOT NULL,
    verification_status TEXT NOT NULL DEFAULT 'VERIFIED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_bond_milestone UNIQUE (user_id, shadow_id, milestone_type)
);

CREATE INDEX IF NOT EXISTS idx_shadow_bond_milestones_user_shadow 
    ON public.shadow_bond_milestones(user_id, shadow_id);
CREATE INDEX IF NOT EXISTS idx_shadow_bond_milestones_type 
    ON public.shadow_bond_milestones(user_id, shadow_id, milestone_type);

-- 4. Enable Row Level Security and explicit per-operation policies (auth.uid() = user_id)
ALTER TABLE public.shadow_bond_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_bond_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_bond_milestones ENABLE ROW LEVEL SECURITY;

-- 4a. shadow_bond_profiles policies
DROP POLICY IF EXISTS "shadow_bond_profiles_select_own" ON public.shadow_bond_profiles;
DROP POLICY IF EXISTS "shadow_bond_profiles_insert_own" ON public.shadow_bond_profiles;
DROP POLICY IF EXISTS "shadow_bond_profiles_update_own" ON public.shadow_bond_profiles;
DROP POLICY IF EXISTS "shadow_bond_profiles_delete_own" ON public.shadow_bond_profiles;

CREATE POLICY "shadow_bond_profiles_select_own" ON public.shadow_bond_profiles
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_bond_profiles_insert_own" ON public.shadow_bond_profiles
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_bond_profiles_update_own" ON public.shadow_bond_profiles
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_bond_profiles_delete_own" ON public.shadow_bond_profiles
    FOR DELETE USING (auth.uid() = user_id);

-- 4b. shadow_bond_events policies
DROP POLICY IF EXISTS "shadow_bond_events_select_own" ON public.shadow_bond_events;
DROP POLICY IF EXISTS "shadow_bond_events_insert_own" ON public.shadow_bond_events;
DROP POLICY IF EXISTS "shadow_bond_events_update_own" ON public.shadow_bond_events;
DROP POLICY IF EXISTS "shadow_bond_events_delete_own" ON public.shadow_bond_events;

CREATE POLICY "shadow_bond_events_select_own" ON public.shadow_bond_events
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_bond_events_insert_own" ON public.shadow_bond_events
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_bond_events_update_own" ON public.shadow_bond_events
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_bond_events_delete_own" ON public.shadow_bond_events
    FOR DELETE USING (auth.uid() = user_id);

-- 4c. shadow_bond_milestones policies
DROP POLICY IF EXISTS "shadow_bond_milestones_select_own" ON public.shadow_bond_milestones;
DROP POLICY IF EXISTS "shadow_bond_milestones_insert_own" ON public.shadow_bond_milestones;
DROP POLICY IF EXISTS "shadow_bond_milestones_update_own" ON public.shadow_bond_milestones;
DROP POLICY IF EXISTS "shadow_bond_milestones_delete_own" ON public.shadow_bond_milestones;

CREATE POLICY "shadow_bond_milestones_select_own" ON public.shadow_bond_milestones
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_bond_milestones_insert_own" ON public.shadow_bond_milestones
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_bond_milestones_update_own" ON public.shadow_bond_milestones
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_bond_milestones_delete_own" ON public.shadow_bond_milestones
    FOR DELETE USING (auth.uid() = user_id);
