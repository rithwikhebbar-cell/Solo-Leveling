-- ============================================================================
-- SOLO LEVELING — REALITY OS
-- MILESTONE 11: THE SHADOW MONARCH SYSTEM
-- Cloud Schema & RLS Policies (PostgreSQL / Supabase)
-- ============================================================================

-- 1. Shadow Monarch Profile (one per user)
CREATE TABLE IF NOT EXISTS public.shadow_monarch_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    monarch_level INTEGER NOT NULL DEFAULT 1,
    monarch_xp BIGINT NOT NULL DEFAULT 0,
    monarch_rank_name TEXT NOT NULL DEFAULT 'AWAKENING',
    monarch_authority INTEGER NOT NULL DEFAULT 10,
    army_capacity INTEGER NOT NULL DEFAULT 5,
    active_shadow_count INTEGER NOT NULL DEFAULT 0,
    total_shadows_recruited INTEGER NOT NULL DEFAULT 0,
    highest_shadow_rank_name TEXT NOT NULL DEFAULT 'E_RANK',
    total_evolutions INTEGER NOT NULL DEFAULT 0,
    shadow_army_power_rating BIGINT NOT NULL DEFAULT 0,
    unlocked_monarch_skill_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_monarch_user UNIQUE (user_id)
);

-- 2. Recruited Shadows
CREATE TABLE IF NOT EXISTS public.shadows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    shadow_id TEXT NOT NULL,
    name TEXT NOT NULL,
    shadow_class_name TEXT NOT NULL,
    rarity_name TEXT NOT NULL,
    current_rank_name TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 1,
    shadow_xp BIGINT NOT NULL DEFAULT 0,
    stat_power INTEGER NOT NULL DEFAULT 10,
    stat_endurance INTEGER NOT NULL DEFAULT 10,
    stat_focus INTEGER NOT NULL DEFAULT 10,
    stat_discipline INTEGER NOT NULL DEFAULT 10,
    stat_wisdom INTEGER NOT NULL DEFAULT 10,
    stat_adaptability INTEGER NOT NULL DEFAULT 10,
    evolution_stage_name TEXT NOT NULL DEFAULT 'NEWLY_EXTRACTED',
    passive_ability TEXT NOT NULL,
    active_skill_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    recruitment_source TEXT NOT NULL,
    extraction_event_id TEXT NOT NULL,
    extracted_at_epoch_ms BIGINT NOT NULL,
    evolution_history_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    deployment_status_name TEXT NOT NULL DEFAULT 'IDLE',
    equipped_formation_id TEXT,
    is_retired BOOLEAN NOT NULL DEFAULT false,
    notes TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_id_per_user UNIQUE (user_id, shadow_id)
);

-- 3. Shadow Extraction Events (Audit & Idempotency)
CREATE TABLE IF NOT EXISTS public.shadow_extractions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    extraction_id TEXT NOT NULL,
    source_type_name TEXT NOT NULL,
    source_event_id TEXT NOT NULL,
    outcome_name TEXT NOT NULL,
    extracted_shadow_id TEXT,
    shadow_name TEXT,
    shadow_class_name TEXT,
    shadow_rarity_name TEXT,
    determinism_seed BIGINT NOT NULL,
    evidence_summary TEXT NOT NULL,
    timestamp_epoch_ms BIGINT NOT NULL,
    is_confirmed BOOLEAN NOT NULL DEFAULT false,
    monarch_xp_awarded INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_extraction_event_per_user UNIQUE (user_id, source_type_name, source_event_id)
);

-- 4. Shadow Skill Unlocks
CREATE TABLE IF NOT EXISTS public.shadow_skill_unlocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    shadow_id TEXT NOT NULL,
    skill_id TEXT NOT NULL,
    unlocked_at_epoch_ms BIGINT NOT NULL,
    evidence_note TEXT NOT NULL DEFAULT '',
    CONSTRAINT uq_shadow_skill_per_user UNIQUE (user_id, shadow_id, skill_id)
);

-- 5. Shadow Deployments
CREATE TABLE IF NOT EXISTS public.shadow_deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    deployment_id TEXT NOT NULL,
    shadow_id TEXT NOT NULL,
    shadow_name TEXT NOT NULL,
    category_name TEXT NOT NULL,
    role TEXT NOT NULL,
    assigned_at_epoch_ms BIGINT NOT NULL,
    recalled_at_epoch_ms BIGINT,
    status_name TEXT NOT NULL DEFAULT 'DEPLOYED',
    notes TEXT NOT NULL DEFAULT '',
    CONSTRAINT uq_deployment_id_per_user UNIQUE (user_id, deployment_id)
);

-- 6. Shadow Formations
CREATE TABLE IF NOT EXISTS public.shadow_formations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    formation_id TEXT NOT NULL,
    name TEXT NOT NULL,
    formation_type_name TEXT NOT NULL,
    description TEXT NOT NULL,
    max_slots INTEGER NOT NULL DEFAULT 5,
    member_shadow_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT false,
    created_at_epoch_ms BIGINT NOT NULL,
    updated_at_epoch_ms BIGINT NOT NULL,
    CONSTRAINT uq_formation_id_per_user UNIQUE (user_id, formation_id)
);

-- 7. Monarch Skill Unlocks
CREATE TABLE IF NOT EXISTS public.monarch_skill_unlocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    skill_id TEXT NOT NULL,
    unlocked_at_epoch_ms BIGINT NOT NULL,
    evidence_note TEXT NOT NULL DEFAULT '',
    CONSTRAINT uq_monarch_skill_per_user UNIQUE (user_id, skill_id)
);

-- 8. Shadow Quest Progress
CREATE TABLE IF NOT EXISTS public.shadow_quest_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    quest_id TEXT NOT NULL,
    current_value INTEGER NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    reward_claimed_at_epoch_ms BIGINT,
    completed_at_epoch_ms BIGINT,
    CONSTRAINT uq_shadow_quest_per_user UNIQUE (user_id, quest_id)
);

-- 9. Shadow Resource Transactions (Append-Only Ledger)
CREATE TABLE IF NOT EXISTS public.shadow_resource_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    transaction_id TEXT NOT NULL,
    resource_type_name TEXT NOT NULL,
    amount INTEGER NOT NULL,
    transaction_type_name TEXT NOT NULL,
    source_event_id TEXT NOT NULL,
    source_description TEXT NOT NULL,
    balance_after INTEGER NOT NULL,
    timestamp_epoch_ms BIGINT NOT NULL,
    CONSTRAINT uq_resource_tx_per_user UNIQUE (user_id, transaction_id)
);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

ALTER TABLE public.shadow_monarch_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_extractions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_skill_unlocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_deployments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_formations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.monarch_skill_unlocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_quest_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_resource_transactions ENABLE ROW LEVEL SECURITY;

-- Policies: Users can only read/write their own records
CREATE POLICY "Users manage own shadow profile" ON public.shadow_monarch_profiles
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users manage own shadows" ON public.shadows
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users manage own shadow extractions" ON public.shadow_extractions
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users manage own shadow skill unlocks" ON public.shadow_skill_unlocks
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users manage own shadow deployments" ON public.shadow_deployments
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users manage own shadow formations" ON public.shadow_formations
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users manage own monarch skill unlocks" ON public.monarch_skill_unlocks
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users manage own shadow quest progress" ON public.shadow_quest_progress
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users manage own shadow resource transactions" ON public.shadow_resource_transactions
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

