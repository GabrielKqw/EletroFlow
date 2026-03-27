CREATE USER eletroflow WITH PASSWORD 'troque_essa_senha';
CREATE DATABASE eletroflow OWNER eletroflow;
GRANT ALL PRIVILEGES ON DATABASE eletroflow TO eletroflow;

\connect eletroflow;

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) PRIMARY KEY,
    discord_id VARCHAR(40) NOT NULL UNIQUE,
    minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,
    minecraft_username VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS vip_plans (
    plan_key VARCHAR(60) PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    luckperms_group VARCHAR(80) NOT NULL,
    duration_days INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(user_id),
    plan_key VARCHAR(60) NOT NULL REFERENCES vip_plans(plan_key),
    amount NUMERIC(12, 2) NOT NULL,
    txid VARCHAR(64) NOT NULL UNIQUE,
    payer_name VARCHAR(80) NOT NULL,
    payer_cpf VARCHAR(14) NOT NULL,
    copy_paste_code VARCHAR(4096) NOT NULL,
    qr_code_base64 TEXT,
    discord_thread_id VARCHAR(40) NOT NULL,
    provider_reference VARCHAR(100),
    provider_event_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    rewarded_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL REFERENCES payments(payment_id),
    provider_event_id VARCHAR(100) NOT NULL UNIQUE,
    confirmed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS vip_grants (
    grant_id VARCHAR(36) PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL UNIQUE REFERENCES payments(payment_id),
    user_id VARCHAR(36) NOT NULL REFERENCES users(user_id),
    plan_key VARCHAR(60) NOT NULL REFERENCES vip_plans(plan_key),
    luckperms_group VARCHAR(80) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    action VARCHAR(60) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_versions (
    version VARCHAR(40) PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payments_status_created_at ON payments(status, created_at);
CREATE INDEX IF NOT EXISTS idx_users_discord_id ON users(discord_id);
CREATE INDEX IF NOT EXISTS idx_users_minecraft_uuid ON users(minecraft_uuid);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_plan_key ON payments(plan_key);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_payment_id ON payment_transactions(payment_id);
CREATE INDEX IF NOT EXISTS idx_vip_grants_user_id ON vip_grants(user_id);
CREATE INDEX IF NOT EXISTS idx_vip_grants_plan_key ON vip_grants(plan_key);
CREATE INDEX IF NOT EXISTS idx_audit_logs_aggregate ON audit_logs(aggregate_type, aggregate_id, created_at);
