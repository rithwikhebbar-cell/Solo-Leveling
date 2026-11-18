-- ============================================================================
-- SOLO LEVELING — REALITY OS
-- MILESTONE 12 — PHASE 6.1: SHADOW ARMY ACCOUNT ISOLATION, RLS & INTEGRITY HARDENING
-- Target tables:
--   1. public.shadow_monarch_profiles
--   2. public.shadows
--   3. public.shadow_extractions
--   4. public.shadow_skill_unlocks
--   5. public.shadow_deployments
--   6. public.shadow_formations
--   7. public.monarch_skill_unlocks
--   8. public.shadow_quest_progress
--   9. public.shadow_resource_transactions
--  10. public.shadow_xp_traces (Phase 6.1 deterministic XP trace ledger)
--
-- LEGACY / OFFLINE VAULT POLICY:
--   Pre-Phase 6.1 local device records remain strictly isolated on-device under
--   'local_vault_player_1' and are never auto-reassigned or silently uploaded to
--   a newly authenticated Supabase account. All cloud rows require auth.uid() = user_id.
-- ============================================================================

-- 1. Create shadow_xp_traces table if not yet present (mirrors Room v16 shadow_xp_traces)
CREATE TABLE IF NOT EXISTS public.shadow_xp_traces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    trace_id TEXT NOT NULL,
    trace_type TEXT NOT NULL,
    source_tag TEXT NOT NULL,
    target_entity_id TEXT NOT NULL DEFAULT '',
    xp_amount INTEGER NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    timestamp_epoch_ms BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_xp_trace_per_user UNIQUE (user_id, trace_type, source_tag)
);

-- 2. Add updated_at column where missing for cursor-based incremental sync
ALTER TABLE public.shadow_extractions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());
ALTER TABLE public.shadow_skill_unlocks
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());
ALTER TABLE public.shadow_deployments
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());
ALTER TABLE public.monarch_skill_unlocks
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());
ALTER TABLE public.shadow_quest_progress
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());
ALTER TABLE public.shadow_resource_transactions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());

-- 3. Drop coarse Milestone 11 FOR ALL policies and enforce explicit per-operation RLS
DROP POLICY IF EXISTS "Users manage own shadow profile" ON public.shadow_monarch_profiles;
DROP POLICY IF EXISTS "Users manage own shadows" ON public.shadows;
DROP POLICY IF EXISTS "Users manage own shadow extractions" ON public.shadow_extractions;
DROP POLICY IF EXISTS "Users manage own shadow skill unlocks" ON public.shadow_skill_unlocks;
DROP POLICY IF EXISTS "Users manage own shadow deployments" ON public.shadow_deployments;
DROP POLICY IF EXISTS "Users manage own shadow formations" ON public.shadow_formations;
DROP POLICY IF EXISTS "Users manage own monarch skill unlocks" ON public.monarch_skill_unlocks;
DROP POLICY IF EXISTS "Users manage own shadow quest progress" ON public.shadow_quest_progress;
DROP POLICY IF EXISTS "Users manage own shadow resource transactions" ON public.shadow_resource_transactions;

ALTER TABLE public.shadow_monarch_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_extractions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_skill_unlocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_deployments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_formations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.monarch_skill_unlocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_quest_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_resource_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_xp_traces ENABLE ROW LEVEL SECURITY;

-- 3a. shadow_monarch_profiles
DROP POLICY IF EXISTS "shadow_monarch_profiles_select_own" ON public.shadow_monarch_profiles;
DROP POLICY IF EXISTS "shadow_monarch_profiles_insert_own" ON public.shadow_monarch_profiles;
DROP POLICY IF EXISTS "shadow_monarch_profiles_update_own" ON public.shadow_monarch_profiles;
DROP POLICY IF EXISTS "shadow_monarch_profiles_delete_own" ON public.shadow_monarch_profiles;
CREATE POLICY "shadow_monarch_profiles_select_own" ON public.shadow_monarch_profiles
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_monarch_profiles_insert_own" ON public.shadow_monarch_profiles
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_monarch_profiles_update_own" ON public.shadow_monarch_profiles
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_monarch_profiles_delete_own" ON public.shadow_monarch_profiles
    FOR DELETE USING (auth.uid() = user_id);

-- 3b. shadows
DROP POLICY IF EXISTS "shadows_select_own" ON public.shadows;
DROP POLICY IF EXISTS "shadows_insert_own" ON public.shadows;
DROP POLICY IF EXISTS "shadows_update_own" ON public.shadows;
DROP POLICY IF EXISTS "shadows_delete_own" ON public.shadows;
CREATE POLICY "shadows_select_own" ON public.shadows
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadows_insert_own" ON public.shadows
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadows_update_own" ON public.shadows
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadows_delete_own" ON public.shadows
    FOR DELETE USING (auth.uid() = user_id);

-- 3c. shadow_extractions
DROP POLICY IF EXISTS "shadow_extractions_select_own" ON public.shadow_extractions;
DROP POLICY IF EXISTS "shadow_extractions_insert_own" ON public.shadow_extractions;
DROP POLICY IF EXISTS "shadow_extractions_update_own" ON public.shadow_extractions;
DROP POLICY IF EXISTS "shadow_extractions_delete_own" ON public.shadow_extractions;
CREATE POLICY "shadow_extractions_select_own" ON public.shadow_extractions
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_extractions_insert_own" ON public.shadow_extractions
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_extractions_update_own" ON public.shadow_extractions
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_extractions_delete_own" ON public.shadow_extractions
    FOR DELETE USING (auth.uid() = user_id);

-- 3d. shadow_skill_unlocks (also verifies parent shadow ownership via EXISTS)
DROP POLICY IF EXISTS "shadow_skill_unlocks_select_own" ON public.shadow_skill_unlocks;
DROP POLICY IF EXISTS "shadow_skill_unlocks_insert_own" ON public.shadow_skill_unlocks;
DROP POLICY IF EXISTS "shadow_skill_unlocks_update_own" ON public.shadow_skill_unlocks;
DROP POLICY IF EXISTS "shadow_skill_unlocks_delete_own" ON public.shadow_skill_unlocks;
CREATE POLICY "shadow_skill_unlocks_select_own" ON public.shadow_skill_unlocks
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_skill_unlocks_insert_own" ON public.shadow_skill_unlocks
    FOR INSERT WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.shadows s
            WHERE s.user_id = auth.uid() AND s.shadow_id = shadow_skill_unlocks.shadow_id
        )
    );
CREATE POLICY "shadow_skill_unlocks_update_own" ON public.shadow_skill_unlocks
    FOR UPDATE USING (auth.uid() = user_id)
    WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.shadows s
            WHERE s.user_id = auth.uid() AND s.shadow_id = shadow_skill_unlocks.shadow_id
        )
    );
CREATE POLICY "shadow_skill_unlocks_delete_own" ON public.shadow_skill_unlocks
    FOR DELETE USING (auth.uid() = user_id);

-- 3e. shadow_deployments (also verifies parent shadow ownership via EXISTS)
DROP POLICY IF EXISTS "shadow_deployments_select_own" ON public.shadow_deployments;
DROP POLICY IF EXISTS "shadow_deployments_insert_own" ON public.shadow_deployments;
DROP POLICY IF EXISTS "shadow_deployments_update_own" ON public.shadow_deployments;
DROP POLICY IF EXISTS "shadow_deployments_delete_own" ON public.shadow_deployments;
CREATE POLICY "shadow_deployments_select_own" ON public.shadow_deployments
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_deployments_insert_own" ON public.shadow_deployments
    FOR INSERT WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.shadows s
            WHERE s.user_id = auth.uid() AND s.shadow_id = shadow_deployments.shadow_id
        )
    );
CREATE POLICY "shadow_deployments_update_own" ON public.shadow_deployments
    FOR UPDATE USING (auth.uid() = user_id)
    WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.shadows s
            WHERE s.user_id = auth.uid() AND s.shadow_id = shadow_deployments.shadow_id
        )
    );
CREATE POLICY "shadow_deployments_delete_own" ON public.shadow_deployments
    FOR DELETE USING (auth.uid() = user_id);

-- 3f. shadow_formations
DROP POLICY IF EXISTS "shadow_formations_select_own" ON public.shadow_formations;
DROP POLICY IF EXISTS "shadow_formations_insert_own" ON public.shadow_formations;
DROP POLICY IF EXISTS "shadow_formations_update_own" ON public.shadow_formations;
DROP POLICY IF EXISTS "shadow_formations_delete_own" ON public.shadow_formations;
CREATE POLICY "shadow_formations_select_own" ON public.shadow_formations
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_formations_insert_own" ON public.shadow_formations
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_formations_update_own" ON public.shadow_formations
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_formations_delete_own" ON public.shadow_formations
    FOR DELETE USING (auth.uid() = user_id);

-- 3g. monarch_skill_unlocks
DROP POLICY IF EXISTS "monarch_skill_unlocks_select_own" ON public.monarch_skill_unlocks;
DROP POLICY IF EXISTS "monarch_skill_unlocks_insert_own" ON public.monarch_skill_unlocks;
DROP POLICY IF EXISTS "monarch_skill_unlocks_update_own" ON public.monarch_skill_unlocks;
DROP POLICY IF EXISTS "monarch_skill_unlocks_delete_own" ON public.monarch_skill_unlocks;
CREATE POLICY "monarch_skill_unlocks_select_own" ON public.monarch_skill_unlocks
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "monarch_skill_unlocks_insert_own" ON public.monarch_skill_unlocks
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "monarch_skill_unlocks_update_own" ON public.monarch_skill_unlocks
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "monarch_skill_unlocks_delete_own" ON public.monarch_skill_unlocks
    FOR DELETE USING (auth.uid() = user_id);

-- 3h. shadow_quest_progress
DROP POLICY IF EXISTS "shadow_quest_progress_select_own" ON public.shadow_quest_progress;
DROP POLICY IF EXISTS "shadow_quest_progress_insert_own" ON public.shadow_quest_progress;
DROP POLICY IF EXISTS "shadow_quest_progress_update_own" ON public.shadow_quest_progress;
DROP POLICY IF EXISTS "shadow_quest_progress_delete_own" ON public.shadow_quest_progress;
CREATE POLICY "shadow_quest_progress_select_own" ON public.shadow_quest_progress
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_quest_progress_insert_own" ON public.shadow_quest_progress
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_quest_progress_update_own" ON public.shadow_quest_progress
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_quest_progress_delete_own" ON public.shadow_quest_progress
    FOR DELETE USING (auth.uid() = user_id);

-- 3i. shadow_resource_transactions
DROP POLICY IF EXISTS "shadow_resource_transactions_select_own" ON public.shadow_resource_transactions;
DROP POLICY IF EXISTS "shadow_resource_transactions_insert_own" ON public.shadow_resource_transactions;
DROP POLICY IF EXISTS "shadow_resource_transactions_update_own" ON public.shadow_resource_transactions;
DROP POLICY IF EXISTS "shadow_resource_transactions_delete_own" ON public.shadow_resource_transactions;
CREATE POLICY "shadow_resource_transactions_select_own" ON public.shadow_resource_transactions
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_resource_transactions_insert_own" ON public.shadow_resource_transactions
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_resource_transactions_update_own" ON public.shadow_resource_transactions
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_resource_transactions_delete_own" ON public.shadow_resource_transactions
    FOR DELETE USING (auth.uid() = user_id);

-- 3j. shadow_xp_traces
DROP POLICY IF EXISTS "shadow_xp_traces_select_own" ON public.shadow_xp_traces;
DROP POLICY IF EXISTS "shadow_xp_traces_insert_own" ON public.shadow_xp_traces;
DROP POLICY IF EXISTS "shadow_xp_traces_update_own" ON public.shadow_xp_traces;
DROP POLICY IF EXISTS "shadow_xp_traces_delete_own" ON public.shadow_xp_traces;
CREATE POLICY "shadow_xp_traces_select_own" ON public.shadow_xp_traces
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_xp_traces_insert_own" ON public.shadow_xp_traces
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_xp_traces_update_own" ON public.shadow_xp_traces
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_xp_traces_delete_own" ON public.shadow_xp_traces
    FOR DELETE USING (auth.uid() = user_id);

-- 4. Ownership Immutability, Child-Parent Consistency & updated_at Triggers
CREATE OR REPLACE FUNCTION public.prevent_shadow_user_id_change()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.user_id IS DISTINCT FROM OLD.user_id THEN
        RAISE EXCEPTION 'Shadow record user_id cannot be reassigned after creation.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.enforce_shadow_child_owner()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    parent_owner UUID;
BEGIN
    SELECT user_id INTO parent_owner
    FROM public.shadows
    WHERE shadow_id = NEW.shadow_id AND user_id = NEW.user_id
    LIMIT 1;

    IF parent_owner IS NULL THEN
        RAISE EXCEPTION 'Shadow child record (shadow_id=%) must reference a shadow owned by user_id=%.', NEW.shadow_id, NEW.user_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.touch_shadow_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := timezone('utc'::text, now());
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_shadow_monarch_profiles_immutable_owner ON public.shadow_monarch_profiles;
CREATE TRIGGER trg_shadow_monarch_profiles_immutable_owner
    BEFORE UPDATE ON public.shadow_monarch_profiles
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_shadows_immutable_owner ON public.shadows;
CREATE TRIGGER trg_shadows_immutable_owner
    BEFORE UPDATE ON public.shadows
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_shadow_extractions_immutable_owner ON public.shadow_extractions;
CREATE TRIGGER trg_shadow_extractions_immutable_owner
    BEFORE UPDATE ON public.shadow_extractions
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_shadow_skill_unlocks_immutable_owner ON public.shadow_skill_unlocks;
CREATE TRIGGER trg_shadow_skill_unlocks_immutable_owner
    BEFORE UPDATE ON public.shadow_skill_unlocks
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_shadow_skill_unlocks_parent_owner ON public.shadow_skill_unlocks;
CREATE TRIGGER trg_shadow_skill_unlocks_parent_owner
    BEFORE INSERT OR UPDATE ON public.shadow_skill_unlocks
    FOR EACH ROW EXECUTE FUNCTION public.enforce_shadow_child_owner();

DROP TRIGGER IF EXISTS trg_shadow_deployments_immutable_owner ON public.shadow_deployments;
CREATE TRIGGER trg_shadow_deployments_immutable_owner
    BEFORE UPDATE ON public.shadow_deployments
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_shadow_deployments_parent_owner ON public.shadow_deployments;
CREATE TRIGGER trg_shadow_deployments_parent_owner
    BEFORE INSERT OR UPDATE ON public.shadow_deployments
    FOR EACH ROW EXECUTE FUNCTION public.enforce_shadow_child_owner();

DROP TRIGGER IF EXISTS trg_shadow_formations_immutable_owner ON public.shadow_formations;
CREATE TRIGGER trg_shadow_formations_immutable_owner
    BEFORE UPDATE ON public.shadow_formations
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_monarch_skill_unlocks_immutable_owner ON public.monarch_skill_unlocks;
CREATE TRIGGER trg_monarch_skill_unlocks_immutable_owner
    BEFORE UPDATE ON public.monarch_skill_unlocks
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_shadow_quest_progress_immutable_owner ON public.shadow_quest_progress;
CREATE TRIGGER trg_shadow_quest_progress_immutable_owner
    BEFORE UPDATE ON public.shadow_quest_progress
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_shadow_resource_tx_immutable_owner ON public.shadow_resource_transactions;
CREATE TRIGGER trg_shadow_resource_tx_immutable_owner
    BEFORE UPDATE ON public.shadow_resource_transactions
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

DROP TRIGGER IF EXISTS trg_shadow_xp_traces_immutable_owner ON public.shadow_xp_traces;
CREATE TRIGGER trg_shadow_xp_traces_immutable_owner
    BEFORE UPDATE ON public.shadow_xp_traces
    FOR EACH ROW EXECUTE FUNCTION public.prevent_shadow_user_id_change();

-- touch_shadow_updated_at triggers across all 10 tables
DROP TRIGGER IF EXISTS trg_shadow_monarch_profiles_updated_at ON public.shadow_monarch_profiles;
CREATE TRIGGER trg_shadow_monarch_profiles_updated_at
    BEFORE INSERT OR UPDATE ON public.shadow_monarch_profiles
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_shadows_updated_at ON public.shadows;
CREATE TRIGGER trg_shadows_updated_at
    BEFORE INSERT OR UPDATE ON public.shadows
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_shadow_extractions_updated_at ON public.shadow_extractions;
CREATE TRIGGER trg_shadow_extractions_updated_at
    BEFORE INSERT OR UPDATE ON public.shadow_extractions
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_shadow_skill_unlocks_updated_at ON public.shadow_skill_unlocks;
CREATE TRIGGER trg_shadow_skill_unlocks_updated_at
    BEFORE INSERT OR UPDATE ON public.shadow_skill_unlocks
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_shadow_deployments_updated_at ON public.shadow_deployments;
CREATE TRIGGER trg_shadow_deployments_updated_at
    BEFORE INSERT OR UPDATE ON public.shadow_deployments
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_shadow_formations_updated_at ON public.shadow_formations;
CREATE TRIGGER trg_shadow_formations_updated_at
    BEFORE INSERT OR UPDATE ON public.shadow_formations
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_monarch_skill_unlocks_updated_at ON public.monarch_skill_unlocks;
CREATE TRIGGER trg_monarch_skill_unlocks_updated_at
    BEFORE INSERT OR UPDATE ON public.monarch_skill_unlocks
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_shadow_quest_progress_updated_at ON public.shadow_quest_progress;
CREATE TRIGGER trg_shadow_quest_progress_updated_at
    BEFORE INSERT OR UPDATE ON public.shadow_quest_progress
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_shadow_resource_tx_updated_at ON public.shadow_resource_transactions;
CREATE TRIGGER trg_shadow_resource_tx_updated_at
    BEFORE INSERT OR UPDATE ON public.shadow_resource_transactions
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

DROP TRIGGER IF EXISTS trg_shadow_xp_traces_updated_at ON public.shadow_xp_traces;
CREATE TRIGGER trg_shadow_xp_traces_updated_at
    BEFORE INSERT OR UPDATE ON public.shadow_xp_traces
    FOR EACH ROW EXECUTE FUNCTION public.touch_shadow_updated_at();

-- 5. Per-Account Uniqueness & Performance Indexes
CREATE UNIQUE INDEX IF NOT EXISTS uq_shadow_res_event_idx
    ON public.shadow_resource_transactions(user_id, source_event_id, resource_type_name, transaction_type_name);
CREATE UNIQUE INDEX IF NOT EXISTS uq_shadow_extractions_id_per_user
    ON public.shadow_extractions(user_id, extraction_id);

CREATE INDEX IF NOT EXISTS idx_shadows_user_active_extracted
    ON public.shadows(user_id, is_retired, extracted_at_epoch_ms DESC);
CREATE INDEX IF NOT EXISTS idx_shadows_user_updated
    ON public.shadows(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_shadow_extractions_user_ts
    ON public.shadow_extractions(user_id, timestamp_epoch_ms DESC);
CREATE INDEX IF NOT EXISTS idx_shadow_extractions_user_updated
    ON public.shadow_extractions(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_shadow_deployments_user_status
    ON public.shadow_deployments(user_id, status_name, assigned_at_epoch_ms DESC);
CREATE INDEX IF NOT EXISTS idx_shadow_deployments_user_shadow
    ON public.shadow_deployments(user_id, shadow_id);
CREATE INDEX IF NOT EXISTS idx_shadow_formations_user_active
    ON public.shadow_formations(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_shadow_resource_tx_user_type_ts
    ON public.shadow_resource_transactions(user_id, resource_type_name, timestamp_epoch_ms DESC);
CREATE INDEX IF NOT EXISTS idx_shadow_xp_traces_user_type_tag
    ON public.shadow_xp_traces(user_id, trace_type, source_tag);

-- 6. Domain & Non-Negative Integrity CHECK Constraints (NOT VALID preserves legacy tolerance while guarding all writes)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_shadow_profile_bounds') THEN
        ALTER TABLE public.shadow_monarch_profiles
            ADD CONSTRAINT chk_shadow_profile_bounds CHECK (
                monarch_level >= 1
                AND monarch_xp >= 0
                AND monarch_authority >= 0
                AND army_capacity >= 1
                AND active_shadow_count >= 0
                AND total_shadows_recruited >= 0
                AND total_evolutions >= 0
                AND shadow_army_power_rating >= 0
            ) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_shadows_bounds') THEN
        ALTER TABLE public.shadows
            ADD CONSTRAINT chk_shadows_bounds CHECK (
                level >= 1
                AND shadow_xp >= 0
                AND stat_power >= 0
                AND stat_endurance >= 0
                AND stat_focus >= 0
                AND stat_discipline >= 0
                AND stat_wisdom >= 0
                AND stat_adaptability >= 0
            ) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_shadow_extractions_xp_non_neg') THEN
        ALTER TABLE public.shadow_extractions
            ADD CONSTRAINT chk_shadow_extractions_xp_non_neg CHECK (monarch_xp_awarded >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_shadow_formations_slots_pos') THEN
        ALTER TABLE public.shadow_formations
            ADD CONSTRAINT chk_shadow_formations_slots_pos CHECK (max_slots >= 1) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_shadow_quest_value_non_neg') THEN
        ALTER TABLE public.shadow_quest_progress
            ADD CONSTRAINT chk_shadow_quest_value_non_neg CHECK (current_value >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_shadow_resource_balance_non_neg') THEN
        ALTER TABLE public.shadow_resource_transactions
            ADD CONSTRAINT chk_shadow_resource_balance_non_neg CHECK (balance_after >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_shadow_xp_traces_positive') THEN
        ALTER TABLE public.shadow_xp_traces
            ADD CONSTRAINT chk_shadow_xp_traces_positive CHECK (xp_amount > 0) NOT VALID;
    END IF;
END $$;
