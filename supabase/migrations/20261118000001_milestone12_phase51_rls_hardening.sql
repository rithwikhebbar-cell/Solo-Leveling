-- ============================================================================
-- MILESTONE 12 - PHASE 5.1: FORWARD-ONLY RLS, INTEGRITY & INDEX HARDENING
-- Target tables: all 10 public.social_* tables
-- Note: Forward-only migration; preserves 20261117000001_milestone12_social_mastery.sql
-- ============================================================================

-- 1. Drop coarse FOR ALL policies from initial migration (idempotent)
DROP POLICY IF EXISTS "Users can manage their own social profiles" ON public.social_profiles;
DROP POLICY IF EXISTS "Users can manage their own social module progress" ON public.social_module_progress;
DROP POLICY IF EXISTS "Users can manage their own social practice sessions" ON public.social_practice_sessions;
DROP POLICY IF EXISTS "Users can manage their own social scenario attempts" ON public.social_scenario_attempts;
DROP POLICY IF EXISTS "Users can manage their own social assessments" ON public.social_assessments;
DROP POLICY IF EXISTS "Users can manage their own social mission records" ON public.social_mission_records;
DROP POLICY IF EXISTS "Users can manage their own recommendations" ON public.social_adaptive_recommendations;
DROP POLICY IF EXISTS "Users can manage their own training plans" ON public.social_weekly_training_plans;
DROP POLICY IF EXISTS "Users can manage their own planned activities" ON public.social_planned_activities;
DROP POLICY IF EXISTS "Users can manage their own competency progress" ON public.social_competency_progress;

-- Ensure RLS is enabled on all 10 tables
ALTER TABLE public.social_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_module_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_practice_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_scenario_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_assessments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_mission_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_adaptive_recommendations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_weekly_training_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_planned_activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_competency_progress ENABLE ROW LEVEL SECURITY;

-- 2. Explicit per-operation policies with strict USING and WITH CHECK
-- 2a. social_profiles
DROP POLICY IF EXISTS "social_profiles_select_own" ON public.social_profiles;
DROP POLICY IF EXISTS "social_profiles_insert_own" ON public.social_profiles;
DROP POLICY IF EXISTS "social_profiles_update_own" ON public.social_profiles;
DROP POLICY IF EXISTS "social_profiles_delete_own" ON public.social_profiles;
CREATE POLICY "social_profiles_select_own" ON public.social_profiles
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_profiles_insert_own" ON public.social_profiles
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_profiles_update_own" ON public.social_profiles
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_profiles_delete_own" ON public.social_profiles
    FOR DELETE USING (auth.uid() = user_id);

-- 2b. social_module_progress
DROP POLICY IF EXISTS "social_module_progress_select_own" ON public.social_module_progress;
DROP POLICY IF EXISTS "social_module_progress_insert_own" ON public.social_module_progress;
DROP POLICY IF EXISTS "social_module_progress_update_own" ON public.social_module_progress;
DROP POLICY IF EXISTS "social_module_progress_delete_own" ON public.social_module_progress;
CREATE POLICY "social_module_progress_select_own" ON public.social_module_progress
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_module_progress_insert_own" ON public.social_module_progress
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_module_progress_update_own" ON public.social_module_progress
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_module_progress_delete_own" ON public.social_module_progress
    FOR DELETE USING (auth.uid() = user_id);

-- 2c. social_practice_sessions
DROP POLICY IF EXISTS "social_practice_sessions_select_own" ON public.social_practice_sessions;
DROP POLICY IF EXISTS "social_practice_sessions_insert_own" ON public.social_practice_sessions;
DROP POLICY IF EXISTS "social_practice_sessions_update_own" ON public.social_practice_sessions;
DROP POLICY IF EXISTS "social_practice_sessions_delete_own" ON public.social_practice_sessions;
CREATE POLICY "social_practice_sessions_select_own" ON public.social_practice_sessions
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_practice_sessions_insert_own" ON public.social_practice_sessions
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_practice_sessions_update_own" ON public.social_practice_sessions
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_practice_sessions_delete_own" ON public.social_practice_sessions
    FOR DELETE USING (auth.uid() = user_id);

-- 2d. social_scenario_attempts
DROP POLICY IF EXISTS "social_scenario_attempts_select_own" ON public.social_scenario_attempts;
DROP POLICY IF EXISTS "social_scenario_attempts_insert_own" ON public.social_scenario_attempts;
DROP POLICY IF EXISTS "social_scenario_attempts_update_own" ON public.social_scenario_attempts;
DROP POLICY IF EXISTS "social_scenario_attempts_delete_own" ON public.social_scenario_attempts;
CREATE POLICY "social_scenario_attempts_select_own" ON public.social_scenario_attempts
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_scenario_attempts_insert_own" ON public.social_scenario_attempts
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_scenario_attempts_update_own" ON public.social_scenario_attempts
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_scenario_attempts_delete_own" ON public.social_scenario_attempts
    FOR DELETE USING (auth.uid() = user_id);

-- 2e. social_assessments
DROP POLICY IF EXISTS "social_assessments_select_own" ON public.social_assessments;
DROP POLICY IF EXISTS "social_assessments_insert_own" ON public.social_assessments;
DROP POLICY IF EXISTS "social_assessments_update_own" ON public.social_assessments;
DROP POLICY IF EXISTS "social_assessments_delete_own" ON public.social_assessments;
CREATE POLICY "social_assessments_select_own" ON public.social_assessments
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_assessments_insert_own" ON public.social_assessments
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_assessments_update_own" ON public.social_assessments
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_assessments_delete_own" ON public.social_assessments
    FOR DELETE USING (auth.uid() = user_id);

-- 2f. social_mission_records
DROP POLICY IF EXISTS "social_mission_records_select_own" ON public.social_mission_records;
DROP POLICY IF EXISTS "social_mission_records_insert_own" ON public.social_mission_records;
DROP POLICY IF EXISTS "social_mission_records_update_own" ON public.social_mission_records;
DROP POLICY IF EXISTS "social_mission_records_delete_own" ON public.social_mission_records;
CREATE POLICY "social_mission_records_select_own" ON public.social_mission_records
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_mission_records_insert_own" ON public.social_mission_records
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_mission_records_update_own" ON public.social_mission_records
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_mission_records_delete_own" ON public.social_mission_records
    FOR DELETE USING (auth.uid() = user_id);

-- 2g. social_adaptive_recommendations
DROP POLICY IF EXISTS "social_adaptive_recommendations_select_own" ON public.social_adaptive_recommendations;
DROP POLICY IF EXISTS "social_adaptive_recommendations_insert_own" ON public.social_adaptive_recommendations;
DROP POLICY IF EXISTS "social_adaptive_recommendations_update_own" ON public.social_adaptive_recommendations;
DROP POLICY IF EXISTS "social_adaptive_recommendations_delete_own" ON public.social_adaptive_recommendations;
CREATE POLICY "social_adaptive_recommendations_select_own" ON public.social_adaptive_recommendations
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_adaptive_recommendations_insert_own" ON public.social_adaptive_recommendations
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_adaptive_recommendations_update_own" ON public.social_adaptive_recommendations
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_adaptive_recommendations_delete_own" ON public.social_adaptive_recommendations
    FOR DELETE USING (auth.uid() = user_id);

-- 2h. social_weekly_training_plans
DROP POLICY IF EXISTS "social_weekly_training_plans_select_own" ON public.social_weekly_training_plans;
DROP POLICY IF EXISTS "social_weekly_training_plans_insert_own" ON public.social_weekly_training_plans;
DROP POLICY IF EXISTS "social_weekly_training_plans_update_own" ON public.social_weekly_training_plans;
DROP POLICY IF EXISTS "social_weekly_training_plans_delete_own" ON public.social_weekly_training_plans;
CREATE POLICY "social_weekly_training_plans_select_own" ON public.social_weekly_training_plans
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_weekly_training_plans_insert_own" ON public.social_weekly_training_plans
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_weekly_training_plans_update_own" ON public.social_weekly_training_plans
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_weekly_training_plans_delete_own" ON public.social_weekly_training_plans
    FOR DELETE USING (auth.uid() = user_id);

-- 2i. social_planned_activities (verify BOTH auth.uid() = user_id AND parent plan ownership)
DROP POLICY IF EXISTS "social_planned_activities_select_own" ON public.social_planned_activities;
DROP POLICY IF EXISTS "social_planned_activities_insert_own" ON public.social_planned_activities;
DROP POLICY IF EXISTS "social_planned_activities_update_own" ON public.social_planned_activities;
DROP POLICY IF EXISTS "social_planned_activities_delete_own" ON public.social_planned_activities;
CREATE POLICY "social_planned_activities_select_own" ON public.social_planned_activities
    FOR SELECT USING (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.social_weekly_training_plans p
            WHERE p.plan_id = social_planned_activities.plan_id
              AND p.user_id = auth.uid()
        )
    );
CREATE POLICY "social_planned_activities_insert_own" ON public.social_planned_activities
    FOR INSERT WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.social_weekly_training_plans p
            WHERE p.plan_id = social_planned_activities.plan_id
              AND p.user_id = auth.uid()
        )
    );
CREATE POLICY "social_planned_activities_update_own" ON public.social_planned_activities
    FOR UPDATE USING (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.social_weekly_training_plans p
            WHERE p.plan_id = social_planned_activities.plan_id
              AND p.user_id = auth.uid()
        )
    )
    WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.social_weekly_training_plans p
            WHERE p.plan_id = social_planned_activities.plan_id
              AND p.user_id = auth.uid()
        )
    );
CREATE POLICY "social_planned_activities_delete_own" ON public.social_planned_activities
    FOR DELETE USING (
        auth.uid() = user_id
        AND EXISTS (
            SELECT 1 FROM public.social_weekly_training_plans p
            WHERE p.plan_id = social_planned_activities.plan_id
              AND p.user_id = auth.uid()
        )
    );

-- 2j. social_competency_progress
DROP POLICY IF EXISTS "social_competency_progress_select_own" ON public.social_competency_progress;
DROP POLICY IF EXISTS "social_competency_progress_insert_own" ON public.social_competency_progress;
DROP POLICY IF EXISTS "social_competency_progress_update_own" ON public.social_competency_progress;
DROP POLICY IF EXISTS "social_competency_progress_delete_own" ON public.social_competency_progress;
CREATE POLICY "social_competency_progress_select_own" ON public.social_competency_progress
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "social_competency_progress_insert_own" ON public.social_competency_progress
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_competency_progress_update_own" ON public.social_competency_progress
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "social_competency_progress_delete_own" ON public.social_competency_progress
    FOR DELETE USING (auth.uid() = user_id);

-- 3. Immutable Ownership Triggers (block user_id reassignment on UPDATE)
CREATE OR REPLACE FUNCTION public.prevent_social_user_id_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.user_id IS DISTINCT FROM NEW.user_id THEN
        RAISE EXCEPTION 'Ownership transfer denied: user_id cannot be altered once created.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.enforce_planned_activity_plan_owner()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    plan_owner UUID;
BEGIN
    SELECT user_id INTO plan_owner
    FROM public.social_weekly_training_plans
    WHERE plan_id = NEW.plan_id;

    IF plan_owner IS NULL OR plan_owner IS DISTINCT FROM NEW.user_id THEN
        RAISE EXCEPTION 'Planned activity user_id must match owning weekly training plan user_id.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_social_profiles_immutable_owner ON public.social_profiles;
CREATE TRIGGER trg_social_profiles_immutable_owner
    BEFORE UPDATE ON public.social_profiles
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_module_progress_immutable_owner ON public.social_module_progress;
CREATE TRIGGER trg_social_module_progress_immutable_owner
    BEFORE UPDATE ON public.social_module_progress
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_practice_sessions_immutable_owner ON public.social_practice_sessions;
CREATE TRIGGER trg_social_practice_sessions_immutable_owner
    BEFORE UPDATE ON public.social_practice_sessions
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_scenario_attempts_immutable_owner ON public.social_scenario_attempts;
CREATE TRIGGER trg_social_scenario_attempts_immutable_owner
    BEFORE UPDATE ON public.social_scenario_attempts
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_assessments_immutable_owner ON public.social_assessments;
CREATE TRIGGER trg_social_assessments_immutable_owner
    BEFORE UPDATE ON public.social_assessments
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_mission_records_immutable_owner ON public.social_mission_records;
CREATE TRIGGER trg_social_mission_records_immutable_owner
    BEFORE UPDATE ON public.social_mission_records
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_recs_immutable_owner ON public.social_adaptive_recommendations;
CREATE TRIGGER trg_social_recs_immutable_owner
    BEFORE UPDATE ON public.social_adaptive_recommendations
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_plans_immutable_owner ON public.social_weekly_training_plans;
CREATE TRIGGER trg_social_plans_immutable_owner
    BEFORE UPDATE ON public.social_weekly_training_plans
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_planned_acts_immutable_owner ON public.social_planned_activities;
CREATE TRIGGER trg_social_planned_acts_immutable_owner
    BEFORE UPDATE ON public.social_planned_activities
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

DROP TRIGGER IF EXISTS trg_social_planned_acts_plan_owner ON public.social_planned_activities;
CREATE TRIGGER trg_social_planned_acts_plan_owner
    BEFORE INSERT OR UPDATE ON public.social_planned_activities
    FOR EACH ROW EXECUTE FUNCTION public.enforce_planned_activity_plan_owner();

DROP TRIGGER IF EXISTS trg_social_comp_prog_immutable_owner ON public.social_competency_progress;
CREATE TRIGGER trg_social_comp_prog_immutable_owner
    BEFORE UPDATE ON public.social_competency_progress
    FOR EACH ROW EXECUTE FUNCTION public.prevent_social_user_id_change();

-- 4. Composite Query Performance Indexes
CREATE INDEX IF NOT EXISTS idx_social_recs_user_status
    ON public.social_adaptive_recommendations(user_id, status);
CREATE INDEX IF NOT EXISTS idx_social_recs_user_created_desc
    ON public.social_adaptive_recommendations(user_id, created_at_epoch_ms DESC);
CREATE INDEX IF NOT EXISTS idx_social_plans_user_start_desc
    ON public.social_weekly_training_plans(user_id, start_date_epoch_ms DESC);
CREATE INDEX IF NOT EXISTS idx_social_planned_acts_plan_user_date
    ON public.social_planned_activities(plan_id, user_id, scheduled_date_epoch_ms);
CREATE INDEX IF NOT EXISTS idx_social_comp_prog_user_module
    ON public.social_competency_progress(user_id, module_name);
CREATE INDEX IF NOT EXISTS idx_social_comp_prog_user_stage
    ON public.social_competency_progress(user_id, mastery_stage);
CREATE INDEX IF NOT EXISTS idx_social_attempts_user_completed_desc
    ON public.social_scenario_attempts(user_id, completed_epoch_ms DESC);
CREATE INDEX IF NOT EXISTS idx_social_missions_user_status
    ON public.social_mission_records(user_id, lifecycle_status);

-- 5. Safe CHECK Constraints (idempotent validation on future writes; NOT VALID avoids failing on legacy anomalies if any)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_practice_duration_non_neg') THEN
        ALTER TABLE public.social_practice_sessions
            ADD CONSTRAINT chk_social_practice_duration_non_neg CHECK (duration_seconds >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_practice_xp_non_neg') THEN
        ALTER TABLE public.social_practice_sessions
            ADD CONSTRAINT chk_social_practice_xp_non_neg CHECK (xp_awarded >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_attempts_score_range') THEN
        ALTER TABLE public.social_scenario_attempts
            ADD CONSTRAINT chk_social_attempts_score_range CHECK (evaluation_score BETWEEN 0 AND 100) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_assessments_scores_range') THEN
        ALTER TABLE public.social_assessments
            ADD CONSTRAINT chk_social_assessments_scores_range CHECK (
                overall_score BETWEEN 0 AND 100
                AND clarity_score BETWEEN 0 AND 100
                AND empathy_score BETWEEN 0 AND 100
                AND assertiveness_score BETWEEN 0 AND 100
                AND coherence_score BETWEEN 0 AND 100
            ) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_missions_xp_non_neg') THEN
        ALTER TABLE public.social_mission_records
            ADD CONSTRAINT chk_social_missions_xp_non_neg CHECK (xp_awarded >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_plans_targets_valid') THEN
        ALTER TABLE public.social_weekly_training_plans
            ADD CONSTRAINT chk_social_plans_targets_valid CHECK (
                target_days_per_week BETWEEN 1 AND 7
                AND target_minutes_per_day BETWEEN 1 AND 240
            ) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_planned_acts_bounds') THEN
        ALTER TABLE public.social_planned_activities
            ADD CONSTRAINT chk_social_planned_acts_bounds CHECK (
                day_of_week BETWEEN 1 AND 7
                AND duration_minutes BETWEEN 1 AND 240
            ) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_comp_prog_bounds') THEN
        ALTER TABLE public.social_competency_progress
            ADD CONSTRAINT chk_social_comp_prog_bounds CHECK (
                practice_count >= 0
                AND (last_evaluated_score IS NULL OR (last_evaluated_score BETWEEN 0 AND 100))
            ) NOT VALID;
    END IF;
END $$;
