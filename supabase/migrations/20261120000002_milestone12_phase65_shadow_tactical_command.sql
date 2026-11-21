-- ============================================================================
-- SOLO LEVELING — REALITY OS
-- MILESTONE 12 — PHASE 6.5: SHADOW ARMY INTELLIGENCE & TACTICAL COMMAND SYSTEM
-- Target tables:
--   1. public.shadow_squads
--   2. public.shadow_squad_members
--   3. public.shadow_mission_assignments
-- ============================================================================

-- 1. Create shadow_squads table (mirrors Room v22 shadow_squads)
CREATE TABLE IF NOT EXISTS public.shadow_squads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    squad_id TEXT NOT NULL,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    formation_type TEXT NOT NULL,
    leader_shadow_id TEXT,
    max_members INTEGER NOT NULL DEFAULT 6,
    is_active BOOLEAN NOT NULL DEFAULT false,
    notes TEXT NOT NULL DEFAULT '',
    created_at_epoch_ms BIGINT NOT NULL,
    updated_at_epoch_ms BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_squad_per_user UNIQUE (user_id, squad_id)
);

CREATE INDEX IF NOT EXISTS idx_shadow_squads_user_squad 
    ON public.shadow_squads(user_id, squad_id);
CREATE INDEX IF NOT EXISTS idx_shadow_squads_user_active 
    ON public.shadow_squads(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_shadow_squads_user_category 
    ON public.shadow_squads(user_id, category);

-- 2. Create shadow_squad_members table (mirrors Room v22 shadow_squad_members)
CREATE TABLE IF NOT EXISTS public.shadow_squad_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    squad_id TEXT NOT NULL,
    shadow_id TEXT NOT NULL,
    position_index INTEGER NOT NULL DEFAULT 0,
    is_leader BOOLEAN NOT NULL DEFAULT false,
    assigned_at_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_squad_member UNIQUE (user_id, squad_id, shadow_id)
);

CREATE INDEX IF NOT EXISTS idx_shadow_squad_members_user_squad 
    ON public.shadow_squad_members(user_id, squad_id);
CREATE INDEX IF NOT EXISTS idx_shadow_squad_members_user_shadow 
    ON public.shadow_squad_members(user_id, shadow_id);

-- 3. Create shadow_mission_assignments table (mirrors Room v22 shadow_mission_assignments)
CREATE TABLE IF NOT EXISTS public.shadow_mission_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    assignment_id TEXT NOT NULL,
    mission_id TEXT NOT NULL,
    mission_title TEXT NOT NULL,
    mission_type TEXT NOT NULL,
    squad_id TEXT,
    assigned_shadow_ids_json TEXT NOT NULL,
    recommended_strategy TEXT NOT NULL,
    suitability_score INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'IN_PROGRESS',
    assigned_at_epoch_ms BIGINT NOT NULL,
    completed_at_epoch_ms BIGINT,
    success_rate_projected INTEGER NOT NULL,
    result_outcome TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_mission_assignment UNIQUE (user_id, assignment_id)
);

CREATE INDEX IF NOT EXISTS idx_shadow_mission_assignments_user_time 
    ON public.shadow_mission_assignments(user_id, assigned_at_epoch_ms);
CREATE INDEX IF NOT EXISTS idx_shadow_mission_assignments_user_mission 
    ON public.shadow_mission_assignments(user_id, mission_id);
CREATE INDEX IF NOT EXISTS idx_shadow_mission_assignments_status 
    ON public.shadow_mission_assignments(user_id, status);

-- 4. Enable Row Level Security and explicit per-operation policies (auth.uid() = user_id)
ALTER TABLE public.shadow_squads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_squad_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_mission_assignments ENABLE ROW LEVEL SECURITY;

-- 4a. shadow_squads policies
DROP POLICY IF EXISTS "shadow_squads_select_own" ON public.shadow_squads;
DROP POLICY IF EXISTS "shadow_squads_insert_own" ON public.shadow_squads;
DROP POLICY IF EXISTS "shadow_squads_update_own" ON public.shadow_squads;
DROP POLICY IF EXISTS "shadow_squads_delete_own" ON public.shadow_squads;

CREATE POLICY "shadow_squads_select_own" ON public.shadow_squads
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_squads_insert_own" ON public.shadow_squads
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_squads_update_own" ON public.shadow_squads
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_squads_delete_own" ON public.shadow_squads
    FOR DELETE USING (auth.uid() = user_id);

-- 4b. shadow_squad_members policies
DROP POLICY IF EXISTS "shadow_squad_members_select_own" ON public.shadow_squad_members;
DROP POLICY IF EXISTS "shadow_squad_members_insert_own" ON public.shadow_squad_members;
DROP POLICY IF EXISTS "shadow_squad_members_update_own" ON public.shadow_squad_members;
DROP POLICY IF EXISTS "shadow_squad_members_delete_own" ON public.shadow_squad_members;

CREATE POLICY "shadow_squad_members_select_own" ON public.shadow_squad_members
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_squad_members_insert_own" ON public.shadow_squad_members
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_squad_members_update_own" ON public.shadow_squad_members
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_squad_members_delete_own" ON public.shadow_squad_members
    FOR DELETE USING (auth.uid() = user_id);

-- 4c. shadow_mission_assignments policies
DROP POLICY IF EXISTS "shadow_mission_assignments_select_own" ON public.shadow_mission_assignments;
DROP POLICY IF EXISTS "shadow_mission_assignments_insert_own" ON public.shadow_mission_assignments;
DROP POLICY IF EXISTS "shadow_mission_assignments_update_own" ON public.shadow_mission_assignments;
DROP POLICY IF EXISTS "shadow_mission_assignments_delete_own" ON public.shadow_mission_assignments;

CREATE POLICY "shadow_mission_assignments_select_own" ON public.shadow_mission_assignments
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_mission_assignments_insert_own" ON public.shadow_mission_assignments
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_mission_assignments_update_own" ON public.shadow_mission_assignments
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_mission_assignments_delete_own" ON public.shadow_mission_assignments
    FOR DELETE USING (auth.uid() = user_id);
