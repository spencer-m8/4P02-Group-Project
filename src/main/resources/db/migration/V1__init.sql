-- V1__init.sql
-- Baseline schema for the project (matches current Neon tables + constraints)

-- Needed for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------
-- users
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.users (
    user_id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email_enc  text NOT NULL,
    password_hash   text NOT NULL,
    role            text NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT users_role_chk CHECK (role = ANY (ARRAY['INSTRUCTOR'::text, 'ADMIN'::text, 'TA'::text]))
);

-- -----------------------------
-- classes
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.classes (
    class_id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_code         text NOT NULL,
    term                text NOT NULL,
    section             text NULL,
    instructor_user_id  uuid NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT classes_instructor_user_id_fkey
        FOREIGN KEY (instructor_user_id) REFERENCES public.users(user_id) ON DELETE RESTRICT
);

-- -----------------------------
-- assignments
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.assignments (
    assignment_id    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id         uuid NOT NULL,
    assignment_name  text NOT NULL,
    assignment_key   char(10) NOT NULL,
    open_time        timestamptz NULL,
    close_time       timestamptz NULL,
    language         text NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT assignments_class_id_fkey
        FOREIGN KEY (class_id) REFERENCES public.classes(class_id) ON DELETE CASCADE,

    CONSTRAINT assignments_key_digits_chk
        CHECK (assignment_key ~ '^[0-9]{10}$'::text),

    CONSTRAINT assignments_language_chk
        CHECK ((language IS NULL) OR (language = ANY (ARRAY['C'::text, 'CPP'::text, 'JAVA'::text])))
);

-- -----------------------------
-- repositories
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.repositories (
    repository_id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    instructor_user_id      uuid NOT NULL,
    repository_name         text NOT NULL,
    repository_type         text NOT NULL,
    imported_zip_key        text NOT NULL,
    zip_sha256              text NOT NULL,
    zip_validation_status   text NOT NULL DEFAULT 'PENDING'::text,
    validation_error        text NULL,
    normalized_key          text NULL,
    normalized_sha256       text NULL,
    purged_at               timestamptz NULL,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT repositories_instructor_user_id_fkey
        FOREIGN KEY (instructor_user_id) REFERENCES public.users(user_id) ON DELETE RESTRICT,

    CONSTRAINT repositories_type_chk
        CHECK (repository_type = ANY (ARRAY['SECONDARY'::text, 'EXCLUSION'::text, 'CURRENT_ASSIGNMENT'::text])),

    CONSTRAINT repositories_zip_validation_chk
        CHECK (zip_validation_status = ANY (ARRAY['PENDING'::text, 'PASSED'::text, 'FAILED'::text]))
);

-- -----------------------------
-- submissions
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.submissions (
    submission_id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id          uuid NOT NULL,
    submission_time        timestamptz NOT NULL DEFAULT now(),
    late                   boolean NULL,
    encrypted_student      text NOT NULL,
    source_zip_key         text NOT NULL,
    normalized_key         text NULL,
    zip_sha256             text NOT NULL,
    normalized_sha256      text NULL,
    language               text NOT NULL,
    zip_validation_status  text NOT NULL DEFAULT 'PENDING'::text,
    validation_error       text NULL,
    submission_status      text NOT NULL DEFAULT 'RECEIVED'::text,
    purged_at              timestamptz NULL,
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT submissions_assignment_id_fkey
        FOREIGN KEY (assignment_id) REFERENCES public.assignments(assignment_id) ON DELETE CASCADE,

    CONSTRAINT submissions_language_chk
        CHECK (language = ANY (ARRAY['C'::text, 'CPP'::text, 'JAVA'::text])),

    CONSTRAINT submissions_zip_validation_chk
        CHECK (zip_validation_status = ANY (ARRAY['PENDING'::text, 'PASSED'::text, 'FAILED'::text])),

    CONSTRAINT submissions_status_chk
        CHECK (submission_status = ANY (ARRAY[
            'RECEIVED'::text, 'VALIDATED'::text, 'PENDING_ANALYSIS'::text,
            'ANALYZING'::text, 'COMPLETE'::text, 'FAILED'::text
        ]))
);

-- -----------------------------
-- repository_items
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.repository_items (
    repo_id     uuid NOT NULL,
    sub_id      uuid NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT repository_items_pkey PRIMARY KEY (repo_id, sub_id),

    CONSTRAINT repository_items_repo_id_fkey
        FOREIGN KEY (repo_id) REFERENCES public.repositories(repository_id) ON DELETE CASCADE,

    CONSTRAINT repository_items_sub_id_fkey
        FOREIGN KEY (sub_id) REFERENCES public.submissions(submission_id) ON DELETE CASCADE
);

-- -----------------------------
-- analyses
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.analyses (
    analysis_id      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    started_by       uuid NOT NULL,
    assignment_id    uuid NULL,
    analysis_status  text NOT NULL DEFAULT 'QUEUED'::text,
    worker_version   text NULL,
    error_message    text NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    start_time       timestamptz NULL,
    end_time         timestamptz NULL,
    updated_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT analyses_started_by_fkey
        FOREIGN KEY (started_by) REFERENCES public.users(user_id) ON DELETE RESTRICT,

    CONSTRAINT analyses_assignment_id_fkey
        FOREIGN KEY (assignment_id) REFERENCES public.assignments(assignment_id) ON DELETE SET NULL
);

-- -----------------------------
-- match_results
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.match_results (
    match_id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id        uuid NOT NULL,
    sub_id_a           uuid NOT NULL,
    sub_id_b           uuid NOT NULL,
    sub_id_low         uuid NULL,
    sub_id_high        uuid NULL,
    similarity_score   numeric NOT NULL,
    evidence_key       text NULL,
    created_at         timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT match_results_analysis_id_fkey
        FOREIGN KEY (analysis_id) REFERENCES public.analyses(analysis_id) ON DELETE CASCADE,

    CONSTRAINT match_results_sub_a_fkey
        FOREIGN KEY (sub_id_a) REFERENCES public.submissions(submission_id) ON DELETE CASCADE,

    CONSTRAINT match_results_sub_b_fkey
        FOREIGN KEY (sub_id_b) REFERENCES public.submissions(submission_id) ON DELETE CASCADE,

    CONSTRAINT match_results_sub_low_fkey
        FOREIGN KEY (sub_id_low) REFERENCES public.submissions(submission_id) ON DELETE SET NULL,

    CONSTRAINT match_results_sub_high_fkey
        FOREIGN KEY (sub_id_high) REFERENCES public.submissions(submission_id) ON DELETE SET NULL
);

-- -----------------------------
-- running_repositories
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.running_repositories (
    analysis_id  uuid NOT NULL,
    repo_id      uuid NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT running_repositories_pkey PRIMARY KEY (analysis_id, repo_id),

    CONSTRAINT running_repositories_analysis_id_fkey
        FOREIGN KEY (analysis_id) REFERENCES public.analyses(analysis_id) ON DELETE CASCADE,

    CONSTRAINT running_repositories_repo_id_fkey
        FOREIGN KEY (repo_id) REFERENCES public.repositories(repository_id) ON DELETE CASCADE
);

-- -----------------------------
-- token_index
-- -----------------------------
CREATE TABLE IF NOT EXISTS public.token_index (
    repo_id     uuid NOT NULL,
    sub_id      uuid NOT NULL,
    token_hash  text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT token_index_pkey PRIMARY KEY (repo_id, sub_id, token_hash),

    CONSTRAINT token_index_repo_id_fkey
        FOREIGN KEY (repo_id) REFERENCES public.repositories(repository_id) ON DELETE CASCADE,

    CONSTRAINT token_index_sub_id_fkey
        FOREIGN KEY (sub_id) REFERENCES public.submissions(submission_id) ON DELETE CASCADE
);
