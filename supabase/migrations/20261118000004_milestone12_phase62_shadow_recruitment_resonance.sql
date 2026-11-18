-- ============================================================================
-- SOLO LEVELING — REALITY OS
-- MILESTONE 12 — PHASE 6.2: SHADOW RECRUITMENT, EVIDENCE CLAIMS & RESONANCE LEDGER
-- Target tables:
--   1. public.shadow_evidence_claims
--   2. public.shadow_resonance_records
-- ============================================================================

-- 1. Create shadow_evidence_claims table (mirrors Room v17 shadow_evidence_claims)
CREATE TABLE IF NOT EXISTS public.shadow_evidence_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    claim_id TEXT NOT NULL,
    source_type TEXT NOT NULL,
    source_record_id TEXT NOT NULL,
    claimed_at_epoch_ms BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_evidence_claim_per_user UNIQUE (user_id, source_type, source_record_id)
);

CREATE INDEX IF NOT EXISTS idx_shadow_evidence_claims_user_source 
    ON public.shadow_evidence_claims(user_id, source_type, source_record_id);
CREATE INDEX IF NOT EXISTS idx_shadow_evidence_claims_claim_id 
    ON public.shadow_evidence_claims(user_id, claim_id);

-- 2. Create shadow_resonance_records table (mirrors Room v17 shadow_resonance_records)
CREATE TABLE IF NOT EXISTS public.shadow_resonance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    resonance_id TEXT NOT NULL,
    archetype_id TEXT NOT NULL,
    resonance_level INTEGER NOT NULL DEFAULT 1,
    resonance_shards INTEGER NOT NULL DEFAULT 0,
    last_resonance_epoch_ms BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_shadow_resonance_record_per_user UNIQUE (user_id, archetype_id)
);

CREATE INDEX IF NOT EXISTS idx_shadow_resonance_records_user_archetype 
    ON public.shadow_resonance_records(user_id, archetype_id);
CREATE INDEX IF NOT EXISTS idx_shadow_resonance_records_resonance_id 
    ON public.shadow_resonance_records(user_id, resonance_id);

-- 3. Enable Row Level Security and explicit per-operation policies (auth.uid() = user_id)
ALTER TABLE public.shadow_evidence_claims ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shadow_resonance_records ENABLE ROW LEVEL SECURITY;

-- 3a. shadow_evidence_claims
DROP POLICY IF EXISTS "shadow_evidence_claims_select_own" ON public.shadow_evidence_claims;
DROP POLICY IF EXISTS "shadow_evidence_claims_insert_own" ON public.shadow_evidence_claims;
DROP POLICY IF EXISTS "shadow_evidence_claims_update_own" ON public.shadow_evidence_claims;
DROP POLICY IF EXISTS "shadow_evidence_claims_delete_own" ON public.shadow_evidence_claims;

CREATE POLICY "shadow_evidence_claims_select_own" ON public.shadow_evidence_claims
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_evidence_claims_insert_own" ON public.shadow_evidence_claims
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_evidence_claims_update_own" ON public.shadow_evidence_claims
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_evidence_claims_delete_own" ON public.shadow_evidence_claims
    FOR DELETE USING (auth.uid() = user_id);

-- 3b. shadow_resonance_records
DROP POLICY IF EXISTS "shadow_resonance_records_select_own" ON public.shadow_resonance_records;
DROP POLICY IF EXISTS "shadow_resonance_records_insert_own" ON public.shadow_resonance_records;
DROP POLICY IF EXISTS "shadow_resonance_records_update_own" ON public.shadow_resonance_records;
DROP POLICY IF EXISTS "shadow_resonance_records_delete_own" ON public.shadow_resonance_records;

CREATE POLICY "shadow_resonance_records_select_own" ON public.shadow_resonance_records
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "shadow_resonance_records_insert_own" ON public.shadow_resonance_records
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_resonance_records_update_own" ON public.shadow_resonance_records
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "shadow_resonance_records_delete_own" ON public.shadow_resonance_records
    FOR DELETE USING (auth.uid() = user_id);
