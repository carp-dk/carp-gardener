-- PostgreSQL schema for the Gardener authentication service.
-- Captures authorization state snapshots and provider access parameters.

CREATE TABLE IF NOT EXISTS authorization_states (
    id uuid PRIMARY KEY,
    type text NOT NULL CHECK (type IN ('oauth1', 'oauth2')),
    user_id text NOT NULL,
    data_source_id text NOT NULL,
    success boolean NOT NULL DEFAULT false,
    application_data text,
    request_token text,
    token_secret text,
    payload jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS authorization_states_user_datasource_idx
    ON authorization_states (data_source_id, user_id);

CREATE TABLE IF NOT EXISTS access_params (
    id uuid PRIMARY KEY,
    type text NOT NULL CHECK (type IN ('oauth1', 'oauth2')),
    internal_user_id text NOT NULL,
    external_user_id text,
    data_source_id text NOT NULL,
    payload jsonb NOT NULL,
    application_data text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS access_params_internal_idx
    ON access_params (data_source_id, internal_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS access_params_external_idx
    ON access_params (data_source_id, external_user_id, created_at DESC)
    WHERE external_user_id IS NOT NULL;
