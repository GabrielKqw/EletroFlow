create table if not exists users (
    user_id varchar(36) primary key,
    discord_id varchar(40) not null unique,
    minecraft_uuid varchar(36) not null unique,
    minecraft_username varchar(16) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists vip_plans (
    plan_key varchar(60) primary key,
    display_name varchar(80) not null,
    amount numeric(12, 2) not null,
    currency varchar(10) not null,
    luckperms_group varchar(80) not null,
    discord_role_id varchar(40),
    duration_days integer not null,
    active boolean not null,
    sort_order integer not null,
    updated_at timestamptz not null
);

create table if not exists payments (
    payment_id varchar(36) primary key,
    user_id varchar(36) not null references users(user_id),
    plan_key varchar(60) not null references vip_plans(plan_key),
    amount numeric(12, 2) not null,
    txid varchar(64) not null unique,
    copy_paste_code varchar(4096) not null,
    qr_code_base64 text,
    discord_thread_id varchar(40) not null,
    provider_reference varchar(100),
    provider_event_id varchar(100),
    status varchar(20) not null,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    confirmed_at timestamptz,
    rewarded_at timestamptz
);

create table if not exists payment_transactions (
    transaction_id varchar(36) primary key,
    payment_id varchar(36) not null references payments(payment_id),
    provider_event_id varchar(100) not null unique,
    confirmed_at timestamptz not null,
    created_at timestamptz not null
);

create table if not exists vip_grants (
    grant_id varchar(36) primary key,
    payment_id varchar(36) not null unique references payments(payment_id),
    user_id varchar(36) not null references users(user_id),
    plan_key varchar(60) not null references vip_plans(plan_key),
    luckperms_group varchar(80) not null,
    discord_role_id varchar(40),
    granted_at timestamptz not null,
    expires_at timestamptz not null,
    status varchar(20) not null
);

create table if not exists audit_logs (
    audit_id varchar(36) primary key,
    aggregate_type varchar(40) not null,
    aggregate_id varchar(36) not null,
    action varchar(60) not null,
    details text,
    created_at timestamptz not null
);

create index if not exists idx_payments_status_created_at on payments(status, created_at);
create index if not exists idx_users_discord_id on users(discord_id);
create index if not exists idx_users_minecraft_uuid on users(minecraft_uuid);
create index if not exists idx_payments_user_id on payments(user_id);
create index if not exists idx_payments_plan_key on payments(plan_key);
create index if not exists idx_payment_transactions_payment_id on payment_transactions(payment_id);
create index if not exists idx_vip_grants_user_id on vip_grants(user_id);
create index if not exists idx_vip_grants_plan_key on vip_grants(plan_key);
create index if not exists idx_audit_logs_aggregate on audit_logs(aggregate_type, aggregate_id, created_at);
