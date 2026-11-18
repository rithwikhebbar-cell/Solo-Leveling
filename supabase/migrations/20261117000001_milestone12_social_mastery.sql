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

-- ============================================================================
-- MILESTONE 12 - PHASE 5: ADAPTIVE MASTERY PATHWAYS & WEEKLY PLANNER
-- ============================================================================

-- 7. Adaptive Social Recommendations
CREATE TABLE IF NOT EXISTS public.social_adaptive_recommendations (
    recommendation_id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    target_module TEXT NOT NULL,
    target_competency TEXT NOT NULL,
    category TEXT NOT NULL,
    explanation TEXT NOT NULL,
    supporting_evidence_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    recommended_activity_id TEXT NOT NULL,
    recommended_activity_type TEXT NOT NULL,
    estimated_duration_minutes INTEGER NOT NULL,
    difficulty TEXT NOT NULL,
    expected_learning_outcome TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'NEW',
    created_at_epoch_ms BIGINT NOT NULL,
    updated_at_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 8. Personalized Weekly Training Plans
CREATE TABLE IF NOT EXISTS public.social_weekly_training_plans (
    plan_id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    weekly_development_objective TEXT NOT NULL,
    target_minutes_per_day INTEGER NOT NULL,
    target_days_per_week INTEGER NOT NULL,
    start_date_epoch_ms BIGINT NOT NULL,
    end_date_epoch_ms BIGINT NOT NULL,
    is_paused BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_epoch_ms BIGINT NOT NULL,
    updated_at_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 9. Planned Training Activities
CREATE TABLE IF NOT EXISTS public.social_planned_activities (
    activity_id TEXT PRIMARY KEY,
    plan_id TEXT NOT NULL REFERENCES public.social_weekly_training_plans(plan_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL,
    scheduled_date_epoch_ms BIGINT NOT NULL,
    module_name TEXT NOT NULL,
    competency_name TEXT NOT NULL,
    activity_type TEXT NOT NULL,
    title TEXT NOT NULL,
    reference_id TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    difficulty TEXT NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    is_skipped BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    completed_epoch_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 10. Competency Development Progress
CREATE TABLE IF NOT EXISTS public.social_competency_progress (
    competency_id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    module_name TEXT NOT NULL,
    competency_name TEXT NOT NULL,
    mastery_stage TEXT NOT NULL,
    recommended_difficulty TEXT NOT NULL,
    practice_count INTEGER NOT NULL DEFAULT 0,
    last_practiced_epoch_ms BIGINT,
    last_evaluated_score INTEGER,
    evidence_classification TEXT NOT NULL,
    updated_at_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- RLS for Phase 5 Tables
ALTER TABLE public.social_adaptive_recommendations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_weekly_training_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_planned_activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_competency_progress ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own recommendations" ON public.social_adaptive_recommendations
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own training plans" ON public.social_weekly_training_plans
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own planned activities" ON public.social_planned_activities
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own competency progress" ON public.social_competency_progress
    FOR ALL USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_social_recs_user_id ON public.social_adaptive_recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_social_plans_user_id ON public.social_weekly_training_plans(user_id);
CREATE INDEX IF NOT EXISTS idx_social_planned_acts_plan_id ON public.social_planned_activities(plan_id);
CREATE INDEX IF NOT EXISTS idx_social_planned_acts_user_id ON public.social_planned_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_social_comp_prog_user_id ON public.social_competency_progress(user_id);
