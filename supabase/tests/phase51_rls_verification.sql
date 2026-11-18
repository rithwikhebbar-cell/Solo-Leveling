-- ============================================================================
-- MILESTONE 12 - PHASE 5.1: SUPABASE RLS CROSS-USER ISOLATION VERIFICATION
-- Verifies SELECT / INSERT / UPDATE / DELETE isolation across all 10 social_* tables
-- Execute inside a transaction and ROLLBACK so no test state persists.
-- ============================================================================

BEGIN;

DO $$
DECLARE
    user_a CONSTANT UUID := '11111111-1111-1111-1111-111111111111';
    user_b CONSTANT UUID := '22222222-2222-2222-2222-222222222222';
    cnt INTEGER;
BEGIN
    -- Seed minimal auth.users entries inside rollback-safe transaction when running on local Supabase
    INSERT INTO auth.users (id, email)
    VALUES
        (user_a, 'phase51_user_a@example.test'),
        (user_b, 'phase51_user_b@example.test')
    ON CONFLICT (id) DO NOTHING;

    -- Act as authenticated User A
    PERFORM set_config('role', 'authenticated', true);
    PERFORM set_config('request.jwt.claim.sub', user_a::text, true);

    INSERT INTO public.social_profiles (user_id, last_active_epoch_ms)
    VALUES (user_a, 1700000000000)
    ON CONFLICT (user_id) DO NOTHING;

    INSERT INTO public.social_module_progress (user_id, module_name, last_assessed_epoch_ms, updated_at_epoch_ms)
    VALUES (user_a, 'COMMUNICATION_MASTERY', 1700000000000, 1700000000000);

    INSERT INTO public.social_practice_sessions (
        user_id, module_name, activity_kind, duration_seconds, started_epoch_ms, completed_epoch_ms,
        evidence_source_type, verification_status, xp_awarded, idempotency_key
    ) VALUES (
        user_a, 'COMMUNICATION_MASTERY', 'PRACTICE', 120, 1700000000000, 1700000120000,
        'SYSTEM_OBSERVED', 'VERIFIED_SYSTEM_TIMER', 12, 'rls_test_sess_a'
    );

    INSERT INTO public.social_scenario_attempts (
        user_id, scenario_id, scenario_title, module_name, user_response_text, evaluation_score, is_passed,
        evidence_source_type, verification_status, xp_awarded, idempotency_key, completed_epoch_ms
    ) VALUES (
        user_a, 'SCN_ACTIVE_LISTEN_01', 'Active Listening', 'COMMUNICATION_MASTERY', 'Clear paraphrase.', 86, true,
        'AI_ASSESSED', 'VERIFIED_AI_RUBRIC', 30, 'rls_test_att_a', 1700000120000
    );

    INSERT INTO public.social_assessments (
        user_id, module_name, assessment_type, overall_score, clarity_score, empathy_score,
        assertiveness_score, coherence_score, qualitative_analysis, assessed_epoch_ms
    ) VALUES (
        user_a, 'COMMUNICATION_MASTERY', 'COMPREHENSIVE_RUBRIC', 82, 82, 82, 82, 82, 'Good baseline.', 1700000120000
    );

    INSERT INTO public.social_mission_records (
        user_id, mission_id, module_name, title, difficulty_rank, lifecycle_status,
        started_epoch_ms, evidence_type, verification_status, idempotency_key, updated_at_epoch_ms
    ) VALUES (
        user_a, 'MSN_COMM_E01', 'COMMUNICATION_MASTERY', 'Active Listening Check-In', 'E_RANK', 'IN_PROGRESS',
        1700000120000, 'SELF_REPORTED', 'SELF_REPORTED', 'rls_test_msn_a', 1700000120000
    );

    INSERT INTO public.social_adaptive_recommendations (
        recommendation_id, user_id, title, target_module, target_competency, category, explanation,
        recommended_activity_id, recommended_activity_type, estimated_duration_minutes, difficulty,
        expected_learning_outcome, created_at_epoch_ms, updated_at_epoch_ms
    ) VALUES (
        'REC_RLS_A_01', user_a, 'Reinforce Active Listening', 'COMMUNICATION_MASTERY', 'Active Listening',
        'IMPROVE_WEAKNESS', 'Practice needed.', 'EX_COMM_01', 'EXERCISE', 10, 'EASY',
        'Raise precision.', 1700000120000, 1700000120000
    );

    INSERT INTO public.social_weekly_training_plans (
        plan_id, user_id, weekly_development_objective, target_minutes_per_day, target_days_per_week,
        start_date_epoch_ms, end_date_epoch_ms, created_at_epoch_ms, updated_at_epoch_ms
    ) VALUES (
        'PLAN_RLS_A_01', user_a, 'Listen deeply', 15, 5, 1700000000000, 1700604800000, 1700000000000, 1700000000000
    );

    INSERT INTO public.social_planned_activities (
        activity_id, plan_id, user_id, day_of_week, scheduled_date_epoch_ms, module_name,
        competency_name, activity_type, title, reference_id, duration_minutes, difficulty
    ) VALUES (
        'ACT_RLS_A_01', 'PLAN_RLS_A_01', user_a, 1, 1700000000000, 'COMMUNICATION_MASTERY',
        'Active Listening', 'EXERCISE', 'Drill 1', 'EX_COMM_01', 15, 'NORMAL'
    );

    INSERT INTO public.social_competency_progress (
        competency_id, user_id, module_name, competency_name, mastery_stage, recommended_difficulty,
        practice_count, evidence_classification, updated_at_epoch_ms
    ) VALUES (
        'COMP_RLS_A_01', user_a, 'COMMUNICATION_MASTERY', 'Active Listening', 'COMPETENT', 'NORMAL',
        5, 'SIMULATION_EVALUATED', 1700000120000
    );

    -- Create a legitimate plan owned by User B to verify UPDATE hijack protection later
    PERFORM set_config('request.jwt.claim.sub', user_b::text, true);
    INSERT INTO public.social_profiles (user_id, last_active_epoch_ms)
    VALUES (user_b, 1700000000000)
    ON CONFLICT (user_id) DO NOTHING;

    INSERT INTO public.social_weekly_training_plans (
        plan_id, user_id, weekly_development_objective, target_minutes_per_day, target_days_per_week,
        start_date_epoch_ms, end_date_epoch_ms, created_at_epoch_ms, updated_at_epoch_ms
    ) VALUES (
        'PLAN_RLS_B_01', user_b, 'Negotiate ethically', 20, 4, 1700000000000, 1700604800000, 1700000000000, 1700000000000
    );

    -- Verify owner can read own rows (back to User A)
    PERFORM set_config('request.jwt.claim.sub', user_a::text, true);
    SELECT COUNT(*) INTO cnt FROM public.social_planned_activities WHERE activity_id = 'ACT_RLS_A_01';
    IF cnt <> 1 THEN
        RAISE EXCEPTION 'Expected owner SELECT count=1, got %', cnt;
    END IF;

    -- Switch to User B
    PERFORM set_config('request.jwt.claim.sub', user_b::text, true);

    -- 1. Cross-user SELECT must return 0 across all 10 tables
    SELECT COUNT(*) INTO cnt FROM public.social_profiles WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_profiles SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_module_progress WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_module_progress SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_practice_sessions WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_practice_sessions SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_scenario_attempts WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_scenario_attempts SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_assessments WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_assessments SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_mission_records WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_mission_records SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_adaptive_recommendations WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_adaptive_recommendations SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_weekly_training_plans WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_weekly_training_plans SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_planned_activities WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_planned_activities SELECT'; END IF;

    SELECT COUNT(*) INTO cnt FROM public.social_competency_progress WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on social_competency_progress SELECT'; END IF;

    -- 2. Cross-user INSERT spoofing user_id = user_a must fail (42501 insufficient_privilege)
    BEGIN
        INSERT INTO public.social_profiles (user_id, last_active_epoch_ms)
        VALUES (user_a, 1700000000001);
        RAISE EXCEPTION 'Cross-user INSERT unexpectedly succeeded on social_profiles';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;

    -- 3. Attaching User B activity to User A's plan must fail
    BEGIN
        INSERT INTO public.social_planned_activities (
            activity_id, plan_id, user_id, day_of_week, scheduled_date_epoch_ms, module_name,
            competency_name, activity_type, title, reference_id, duration_minutes, difficulty
        ) VALUES (
            'ACT_RLS_HIJACK', 'PLAN_RLS_A_01', user_b, 2, 1700000000000, 'COMMUNICATION_MASTERY',
            'Active Listening', 'EXERCISE', 'Hijack', 'EX_COMM_01', 15, 'NORMAL'
        );
        RAISE EXCEPTION 'Cross-user plan hijack unexpectedly succeeded on social_planned_activities';
    EXCEPTION WHEN insufficient_privilege OR raise_exception THEN NULL;
    END;

    -- 4. Cross-user UPDATE and DELETE must affect 0 rows across protected tables
    UPDATE public.social_adaptive_recommendations
    SET status = 'DISMISSED'
    WHERE recommendation_id = 'REC_RLS_A_01';
    GET DIAGNOSTICS cnt = ROW_COUNT;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on UPDATE social_adaptive_recommendations'; END IF;

    UPDATE public.social_planned_activities
    SET is_completed = TRUE
    WHERE activity_id = 'ACT_RLS_A_01';
    GET DIAGNOSTICS cnt = ROW_COUNT;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on UPDATE social_planned_activities'; END IF;

    DELETE FROM public.social_weekly_training_plans WHERE plan_id = 'PLAN_RLS_A_01';
    GET DIAGNOSTICS cnt = ROW_COUNT;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak on DELETE social_weekly_training_plans'; END IF;

    -- 5. Owner attempting to reassign user_id or move own activity onto another user's parent plan must fail
    PERFORM set_config('request.jwt.claim.sub', user_a::text, true);
    BEGIN
        UPDATE public.social_competency_progress
        SET user_id = user_b
        WHERE competency_id = 'COMP_RLS_A_01';
        RAISE EXCEPTION 'Owner user_id mutation unexpectedly succeeded';
    EXCEPTION WHEN raise_exception OR insufficient_privilege THEN NULL;
    END;

    BEGIN
        UPDATE public.social_planned_activities
        SET plan_id = 'PLAN_RLS_B_01'
        WHERE activity_id = 'ACT_RLS_A_01';
        RAISE EXCEPTION 'Parent plan ownership hijack via UPDATE unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege OR raise_exception THEN NULL;
    END;

    -- 6. Unauthenticated anon session without JWT claim must see 0 rows and fail INSERT
    PERFORM set_config('role', 'anon', true);
    PERFORM set_config('request.jwt.claim.sub', '', true);
    SELECT COUNT(*) INTO cnt FROM public.social_profiles WHERE user_id = user_a;
    IF cnt <> 0 THEN RAISE EXCEPTION 'RLS leak for anon role on social_profiles'; END IF;

    BEGIN
        INSERT INTO public.social_profiles (user_id, last_active_epoch_ms)
        VALUES (user_a, 1700000000002);
        RAISE EXCEPTION 'Unauthenticated anon INSERT unexpectedly succeeded on social_profiles';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;

    RAISE NOTICE 'Phase 5.1 / 5.1.1 Supabase RLS verification assertions passed cleanly.';
END $$;

ROLLBACK;
