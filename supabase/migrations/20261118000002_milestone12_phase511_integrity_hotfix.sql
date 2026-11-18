-- ============================================================================
-- MILESTONE 12 - PHASE 5.1.1: FORWARD-ONLY INTEGRITY HOTFIX MIGRATION
-- Target tables: public.social_competency_progress, public.social_mission_records
-- Purpose:
--   1. Preserve legacy maintenance flags separately while removing legacy MAINTENANCE
--      stage without granting unearned MASTERY_CANDIDATE.
--   2. Add artifact validation metadata columns & owner association check constraint.
-- ============================================================================

-- 1. Competency maintenance preservation + safe legacy MAINTENANCE cleanup
ALTER TABLE public.social_competency_progress
    ADD COLUMN IF NOT EXISTS maintenance_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS maintenance_reason TEXT;

UPDATE public.social_competency_progress
SET mastery_stage = 'UNEXPLORED',
    maintenance_required = TRUE,
    maintenance_reason = COALESCE(
        maintenance_reason,
        'Legacy MAINTENANCE status safely downgraded; historical mastery must be recovered from verified activity logs.'
    )
WHERE UPPER(trim(mastery_stage)) = 'MAINTENANCE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_comp_stage_valid'
    ) THEN
        ALTER TABLE public.social_competency_progress
            ADD CONSTRAINT chk_social_comp_stage_valid CHECK (
                mastery_stage IN (
                    'UNEXPLORED',
                    'BEGINNER',
                    'DEVELOPING',
                    'COMPETENT',
                    'ADVANCED',
                    'MASTERY_CANDIDATE'
                )
            ) NOT VALID;
    END IF;
END $$;

-- 2. Real-World Mission Artifact Reference, Integrity Digest & Account Ownership
ALTER TABLE public.social_mission_records
    ADD COLUMN IF NOT EXISTS artifact_uri TEXT,
    ADD COLUMN IF NOT EXISTS artifact_owner_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS artifact_content_hash TEXT,
    ADD COLUMN IF NOT EXISTS artifact_state_name TEXT NOT NULL DEFAULT 'NONE';

UPDATE public.social_mission_records
SET artifact_state_name = 'ARTIFACT_DESCRIBED',
    evidence_type = CASE
        WHEN UPPER(evidence_type) IN ('ARTIFACT_ATTACHED', 'OPTIONAL_ARTIFACT') THEN 'ARTIFACT_DESCRIBED'
        ELSE evidence_type
    END
WHERE artifact_note IS NOT NULL
  AND trim(artifact_note) <> ''
  AND artifact_uri IS NULL
  AND artifact_state_name = 'NONE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_social_mission_artifact_owner_matches'
    ) THEN
        ALTER TABLE public.social_mission_records
            ADD CONSTRAINT chk_social_mission_artifact_owner_matches CHECK (
                artifact_owner_id IS NULL OR artifact_owner_id = user_id
            ) NOT VALID;
    END IF;
END $$;
