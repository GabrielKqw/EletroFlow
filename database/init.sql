CREATE USER eletroflow WITH PASSWORD 'troque_essa_senha';
CREATE DATABASE eletroflow OWNER eletroflow;
GRANT ALL PRIVILEGES ON DATABASE eletroflow TO eletroflow;

\connect eletroflow;

CREATE TABLE IF NOT EXISTS vip_plans (
    plan_key VARCHAR(60) PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    luckperms_group VARCHAR(80) NOT NULL,
    discord_role_id VARCHAR(40),
    duration_days INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(36) PRIMARY KEY,
    discord_id VARCHAR(40) NOT NULL,
    minecraft_uuid VARCHAR(36) NOT NULL,
    minecraft_username VARCHAR(16) NOT NULL,
    plan_key VARCHAR(60) NOT NULL REFERENCES vip_plans(plan_key),
    amount NUMERIC(12, 2) NOT NULL,
    txid VARCHAR(64) NOT NULL UNIQUE,
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

CREATE INDEX IF NOT EXISTS idx_payments_status_created_at ON payments(status, created_at);
CREATE INDEX IF NOT EXISTS idx_payments_discord_id ON payments(discord_id);
