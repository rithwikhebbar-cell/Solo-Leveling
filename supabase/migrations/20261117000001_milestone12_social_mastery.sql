-- ============================================================================
-- MILESTONE 12: SOCIAL MASTERY SCHEMA EXTENSION
-- ============================================================================

-- 1. Social Profiles
CREATE TABLE IF NOT EXISTS public.social_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    overall_mastery_stage TEXT NOT NULL DEFAULT 'NOVICE',
    total_practice_minutes INTEGER NOT NULL DEFAULT 0,
    total_scenarios_cleared INTEGER NOT NULL DEFAULT 0,
    total_assessments_completed INTEGER NOT NULL DEFAULT 0,
    average_rubric_score REAL NOT NULL DEFAULT 0.0,
    last_active_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 2. Social Module Progress
CREATE TABLE IF NOT EXISTS public.social_module_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    module_name TEXT NOT NULL,
    mastery_stage TEXT NOT NULL DEFAULT 'NOVICE',
    mastery_stage_ordinal INTEGER NOT NULL DEFAULT 0,
    practice_seconds BIGINT NOT NULL DEFAULT 0,
    assessments_completed INTEGER NOT NULL DEFAULT 0,
    rolling_average_score REAL NOT NULL DEFAULT 0.0,
    last_score_std_dev REAL NOT NULL DEFAULT 0.0,
    consecutive_passing_scores INTEGER NOT NULL DEFAULT 0,
    is_stale BOOLEAN NOT NULL DEFAULT false,
    decayed_from_stage TEXT,
    consecutive_restoration_passing_scores INTEGER NOT NULL DEFAULT 0,
    last_assessed_epoch_ms BIGINT NOT NULL,
    last_decay_epoch_ms BIGINT,
    next_decay_eligible_epoch_ms BIGINT,
    updated_at_epoch_ms BIGINT NOT NULL,
    UNIQUE (user_id, module_name)
);

-- 3. Social Practice Sessions (Immutable append-only logs)
-- Note: Raw audio files are NEVER synced to cloud storage.
CREATE TABLE IF NOT EXISTS public.social_practice_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    module_name TEXT NOT NULL,
    activity_kind TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL,
    started_epoch_ms BIGINT NOT NULL,
    completed_epoch_ms BIGINT NOT NULL,
    transcript_text TEXT, -- Nullified if transcript cloud consent is revoked or absent
    transcript_sync_allowed BOOLEAN NOT NULL DEFAULT false,
    evidence_source_type TEXT NOT NULL,
    verification_status TEXT NOT NULL,
    xp_awarded INTEGER NOT NULL,
    idempotency_key TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 4. Social Scenario Attempts
CREATE TABLE IF NOT EXISTS public.social_scenario_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    scenario_id TEXT NOT NULL,
    scenario_title TEXT NOT NULL,
    module_name TEXT NOT NULL,
    user_response_text TEXT NOT NULL,
    evaluation_score INTEGER NOT NULL,
    is_passed BOOLEAN NOT NULL,
    strengths_feedback JSONB NOT NULL DEFAULT '[]'::jsonb,
    improvement_areas JSONB NOT NULL DEFAULT '[]'::jsonb,
    evidence_source_type TEXT NOT NULL,
    verification_status TEXT NOT NULL,
    xp_awarded INTEGER NOT NULL,
    idempotency_key TEXT NOT NULL UNIQUE,
    completed_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 5. Social Assessments
CREATE TABLE IF NOT EXISTS public.social_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    module_name TEXT NOT NULL,
    assessment_type TEXT NOT NULL,
    overall_score INTEGER NOT NULL,
    clarity_score INTEGER NOT NULL,
    empathy_score INTEGER NOT NULL,
    assertiveness_score INTEGER NOT NULL,
    coherence_score INTEGER NOT NULL,
    qualitative_analysis TEXT NOT NULL,
    actionable_recommendations JSONB NOT NULL DEFAULT '[]'::jsonb,
    assessed_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 6. Social Mission Records (Milestone 12 - Phase 4)
CREATE TABLE IF NOT EXISTS public.social_mission_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mission_id TEXT NOT NULL,
    module_name TEXT NOT NULL,
    title TEXT NOT NULL,
    difficulty_rank TEXT NOT NULL,
    lifecycle_status TEXT NOT NULL,
    started_epoch_ms BIGINT NOT NULL,
    submitted_epoch_ms BIGINT,
    completed_epoch_ms BIGINT,
    evidence_type TEXT NOT NULL,
    reflection_text TEXT,
    human_confirming_role TEXT,
    artifact_note TEXT,
    ai_feedback_notes TEXT,
    evaluation_rubric_score INTEGER,
    xp_awarded INTEGER NOT NULL DEFAULT 0,
    verification_status TEXT NOT NULL,
    idempotency_key TEXT NOT NULL UNIQUE,
    updated_at_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
ALTER TABLE public.social_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_module_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_practice_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_scenario_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_assessments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_mission_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own social profiles" ON public.social_profiles
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own social module progress" ON public.social_module_progress
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own social practice sessions" ON public.social_practice_sessions
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own social scenario attempts" ON public.social_scenario_attempts
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own social assessments" ON public.social_assessments
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own social mission records" ON public.social_mission_records
    FOR ALL USING (auth.uid() = user_id);

-- Indices for low-latency queries
CREATE INDEX IF NOT EXISTS idx_social_module_progress_user_id ON public.social_module_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_social_practice_sessions_user_id ON public.social_practice_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_social_scenario_attempts_user_id ON public.social_scenario_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_social_assessments_user_id ON public.social_assessments(user_id);
CREATE INDEX IF NOT EXISTS idx_social_mission_records_user_id ON public.social_mission_records(user_id);
CREATE INDEX IF NOT EXISTS idx_social_mission_records_mission_id ON public.social_mission_records(mission_id);
CREATE INDEX IF NOT EXISTS idx_social_mission_records_lifecycle_status ON public.social_mission_records(lifecycle_status);
