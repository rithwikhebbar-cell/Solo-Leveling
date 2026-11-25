-- ============================================================================
-- SOLO LEVELING — ACADEMIC SUBJECTS RLS & UPSERT SYNC HARDENING
-- Forward-only migration: preserves public.academic_subjects and all data.
-- Replaces coarse FOR ALL policy with explicit per-operation RLS policies,
-- adds immutable user_id ownership guard, and attaches updated_at trigger.
-- ============================================================================

-- 1. Ensure RLS remains strictly enabled
ALTER TABLE public.academic_subjects ENABLE ROW LEVEL SECURITY;

-- 2. Replace coarse FOR ALL policy with explicit per-operation policies
DROP POLICY IF EXISTS "academic_subjects_all_own" ON public.academic_subjects;
DROP POLICY IF EXISTS "academic_subjects_select_own" ON public.academic_subjects;
DROP POLICY IF EXISTS "academic_subjects_insert_own" ON public.academic_subjects;
DROP POLICY IF EXISTS "academic_subjects_update_own" ON public.academic_subjects;
DROP POLICY IF EXISTS "academic_subjects_delete_own" ON public.academic_subjects;

CREATE POLICY "academic_subjects_select_own" ON public.academic_subjects
    FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "academic_subjects_insert_own" ON public.academic_subjects
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "academic_subjects_update_own" ON public.academic_subjects
    FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "academic_subjects_delete_own" ON public.academic_subjects
    FOR DELETE
    USING (auth.uid() = user_id);

-- 3. Immutable ownership trigger: block user_id reassignment on UPDATE / UPSERT
CREATE OR REPLACE FUNCTION public.prevent_academic_subject_user_id_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.user_id IS DISTINCT FROM NEW.user_id THEN
        RAISE EXCEPTION 'Ownership transfer denied: user_id on academic_subjects cannot be altered.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_academic_subjects_immutable_owner ON public.academic_subjects;
CREATE TRIGGER trg_academic_subjects_immutable_owner
    BEFORE UPDATE ON public.academic_subjects
    FOR EACH ROW EXECUTE FUNCTION public.prevent_academic_subject_user_id_change();

-- 4. Keep updated_at current on UPDATE / UPSERT for incremental sync cursors
DROP TRIGGER IF EXISTS trg_academic_subjects_updated ON public.academic_subjects;
CREATE TRIGGER trg_academic_subjects_updated
    BEFORE UPDATE ON public.academic_subjects
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
