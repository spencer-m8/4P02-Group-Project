-- Enable so UUIDs can be generated with gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Whenever a row is updated sets updated_at to the current time
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- Users who can access/manage the system
CREATE TABLE IF NOT EXISTS public.users (
    user_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email_enc text NOT NULL,
    password_hash text NOT NULL,
    role text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_role_chk
        CHECK (role = ANY (ARRAY['INSTRUCTOR'::text, 'ADMIN'::text, 'TA'::text]))
);

-- Classes/courses in the system.
CREATE TABLE IF NOT EXISTS public.classes (
    class_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_code text NOT NULL,
    term text NOT NULL,
    section text,
    instructor_user_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT classes_instructor_user_id_fkey
        FOREIGN KEY (instructor_user_id) REFERENCES public.users(user_id) ON DELETE RESTRICT
);

-- Assignments for each class.
CREATE TABLE IF NOT EXISTS public.assignments (
    assignment_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id uuid NOT NULL,
    assignment_name text NOT NULL,
    assignment_key char(10) NOT NULL,
    open_time timestamptz,
    close_time timestamptz,
    language text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT assignments_class_id_fkey
        FOREIGN KEY (class_id) REFERENCES public.classes(class_id) ON DELETE CASCADE,
    CONSTRAINT assignments_key_digits_chk
        CHECK (assignment_key ~ '^[0-9]{10}$'::text),
    CONSTRAINT assignments_language_chk
        CHECK ((language IS NULL) OR (language = ANY (ARRAY['C'::text, 'CPP'::text, 'JAVA'::text]))),
    CONSTRAINT assignments_assignment_key_uk
        UNIQUE (assignment_key)
);

-- Repositories used by the system for comparison
CREATE TABLE IF NOT EXISTS public.repositories (
    repository_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    instructor_user_id uuid NOT NULL,
    repository_name text NOT NULL,
    repository_type text NOT NULL,
    imported_zip_key text NOT NULL,
    zip_sha256 text NOT NULL,
    zip_validation_status text NOT NULL DEFAULT 'PENDING'::text,
    validation_error text,
    normalized_key text,
    normalized_sha256 text,
    purged_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT repositories_instructor_user_id_fkey
        FOREIGN KEY (instructor_user_id) REFERENCES public.users(user_id) ON DELETE RESTRICT,
    CONSTRAINT repositories_type_chk
        CHECK (repository_type = ANY (ARRAY['SECONDARY'::text, 'EXCLUSION'::text, 'CURRENT_ASSIGNMENT'::text])),
    CONSTRAINT repositories_zip_validation_chk
        CHECK (zip_validation_status = ANY (ARRAY['PENDING'::text, 'PASSED'::text, 'FAILED'::text]))
);

-- Student submissions
CREATE TABLE IF NOT EXISTS public.submissions (
    submission_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id uuid NOT NULL,
    submission_time timestamptz NOT NULL DEFAULT now(),
    late boolean,
    encrypted_student text NOT NULL,
    source_zip_key text NOT NULL,
    normalized_key text,
    zip_sha256 text NOT NULL,
    normalized_sha256 text,
    language text NOT NULL,
    zip_validation_status text NOT NULL DEFAULT 'PENDING'::text,
    validation_error text,
    submission_status text NOT NULL DEFAULT 'RECEIVED'::text,
    purged_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT submissions_assignment_id_fkey
        FOREIGN KEY (assignment_id) REFERENCES public.assignments(assignment_id) ON DELETE CASCADE,
    CONSTRAINT submissions_language_chk
        CHECK (language = ANY (ARRAY['C'::text, 'CPP'::text, 'JAVA'::text])),
    CONSTRAINT submissions_status_chk
        CHECK (submission_status = ANY (ARRAY[
            'RECEIVED'::text,
            'VALIDATED'::text,
            'PENDING_ANALYSIS'::text,
            'ANALYZING'::text,
            'COMPLETE'::text,
            'FAILED'::text
        ])),
    CONSTRAINT submissions_zip_validation_chk
        CHECK (zip_validation_status = ANY (ARRAY['PENDING'::text, 'PASSED'::text, 'FAILED'::text]))
);

-- Table linking repos to submissions.
CREATE TABLE IF NOT EXISTS public.repository_items (
    repo_id uuid NOT NULL,
    sub_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT repository_items_pkey
        PRIMARY KEY (repo_id, sub_id),
    CONSTRAINT repository_items_repo_id_fkey
        FOREIGN KEY (repo_id) REFERENCES public.repositories(repository_id) ON DELETE CASCADE,
    CONSTRAINT repository_items_sub_id_fkey
        FOREIGN KEY (sub_id) REFERENCES public.submissions(submission_id) ON DELETE CASCADE
);

-- Used to track details of comparison runs
CREATE TABLE IF NOT EXISTS public.analyses (
    analysis_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    started_by uuid NOT NULL,
    assignment_id uuid,
    analysis_status text NOT NULL DEFAULT 'QUEUED'::text,
    worker_version text,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    start_time timestamptz,
    end_time timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT analyses_status_chk
        CHECK (analysis_status = ANY (ARRAY['QUEUED'::text, 'RUNNING'::text, 'COMPLETE'::text, 'FAILED'::text])),
    CONSTRAINT analyses_assignment_id_fkey
        FOREIGN KEY (assignment_id) REFERENCES public.assignments(assignment_id) ON DELETE CASCADE,
    CONSTRAINT analyses_started_by_fkey
        FOREIGN KEY (started_by) REFERENCES public.users(user_id) ON DELETE RESTRICT
);

-- Saved comparison results between two submissions
CREATE TABLE IF NOT EXISTS public.match_results (
    match_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id uuid NOT NULL,
    sub_id_a uuid NOT NULL,
    sub_id_b uuid NOT NULL,
    sub_id_low uuid,
    sub_id_high uuid,
    similarity_score numeric(5,2) NOT NULL,
    evidence_key text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT match_results_pair_chk
        CHECK (sub_id_a <> sub_id_b),
    CONSTRAINT match_results_score_chk
        CHECK (similarity_score >= 0 AND similarity_score <= 100),
    CONSTRAINT match_results_analysis_id_fkey
        FOREIGN KEY (analysis_id) REFERENCES public.analyses(analysis_id) ON DELETE CASCADE,
    CONSTRAINT match_results_sub_id_a_fkey
        FOREIGN KEY (sub_id_a) REFERENCES public.submissions(submission_id) ON DELETE RESTRICT,
    CONSTRAINT match_results_sub_id_b_fkey
        FOREIGN KEY (sub_id_b) REFERENCES public.submissions(submission_id) ON DELETE RESTRICT
);

-- Table tracking which repositories were used in a which runs
CREATE TABLE IF NOT EXISTS public.running_repositories (
    analysis_id uuid NOT NULL,
    repo_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT running_repositories_pkey
        PRIMARY KEY (analysis_id, repo_id),
    CONSTRAINT running_repositories_analysis_id_fkey
        FOREIGN KEY (analysis_id) REFERENCES public.analyses(analysis_id) ON DELETE CASCADE,
    CONSTRAINT running_repositories_repo_id_fkey
        FOREIGN KEY (repo_id) REFERENCES public.repositories(repository_id) ON DELETE RESTRICT
);

-- Stores token/fingerprints used by the comparison engine
CREATE TABLE IF NOT EXISTS public.token_index (
    repo_id uuid NOT NULL,
    sub_id uuid NOT NULL,
    token_hash text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT token_index_pkey
        PRIMARY KEY (repo_id, sub_id, token_hash),
    CONSTRAINT token_index_repo_id_fkey
        FOREIGN KEY (repo_id) REFERENCES public.repositories(repository_id) ON DELETE CASCADE,
    CONSTRAINT token_index_sub_id_fkey
        FOREIGN KEY (sub_id) REFERENCES public.submissions(submission_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS users_email_unique_idx
    ON public.users USING btree (user_email_enc);

CREATE INDEX IF NOT EXISTS idx_classes_course_code
    ON public.classes USING btree (course_code);

CREATE INDEX IF NOT EXISTS assignments_class_idx
    ON public.assignments USING btree (class_id);

CREATE INDEX IF NOT EXISTS submissions_assignment_idx
    ON public.submissions USING btree (assignment_id);

CREATE INDEX IF NOT EXISTS idx_submissions_assignment_student_time
    ON public.submissions USING btree (assignment_id, encrypted_student, created_at DESC);

CREATE INDEX IF NOT EXISTS repository_items_sub_idx
    ON public.repository_items USING btree (sub_id);

CREATE INDEX IF NOT EXISTS analyses_assignment_idx
    ON public.analyses USING btree (assignment_id);

CREATE INDEX IF NOT EXISTS match_results_analysis_idx
    ON public.match_results USING btree (analysis_id);

CREATE UNIQUE INDEX IF NOT EXISTS match_results_unique_pair_idx
    ON public.match_results USING btree (analysis_id, sub_id_low, sub_id_high);

CREATE INDEX IF NOT EXISTS running_repositories_repo_idx
    ON public.running_repositories USING btree (repo_id);

CREATE INDEX IF NOT EXISTS token_index_lookup_idx
    ON public.token_index USING btree (repo_id, token_hash);

CREATE INDEX IF NOT EXISTS token_index_sub_idx
    ON public.token_index USING btree (sub_id);


DROP TRIGGER IF EXISTS trg_users_updated_at ON public.users;
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS trg_classes_updated_at ON public.classes;
CREATE TRIGGER trg_classes_updated_at
    BEFORE UPDATE ON public.classes
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS trg_assignments_updated_at ON public.assignments;
CREATE TRIGGER trg_assignments_updated_at
    BEFORE UPDATE ON public.assignments
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS trg_repositories_updated_at ON public.repositories;
CREATE TRIGGER trg_repositories_updated_at
    BEFORE UPDATE ON public.repositories
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS trg_submissions_updated_at ON public.submissions;
CREATE TRIGGER trg_submissions_updated_at
    BEFORE UPDATE ON public.submissions
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS trg_analyses_updated_at ON public.analyses;
CREATE TRIGGER trg_analyses_updated_at
    BEFORE UPDATE ON public.analyses
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();