create table users (
    id varchar(36) primary key,
    discord_id varchar(40) not null unique,
    minecraft_uuid varchar(36) unique,
    minecraft_username varchar(16),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table vip_plans (
    id varchar(60) primary key,
    display_name varchar(80) not null,
    amount numeric(12, 2) not null,
    currency varchar(10) not null,
    luckperms_group varchar(80) not null,
    discord_role_id varchar(40),
    duration_days integer not null,
    active boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table payments (
    id varchar(36) primary key,
    user_id varchar(36) not null references users(id),
    plan_id varchar(60) not null references vip_plans(id),
    amount numeric(12, 2) not null,
    status varchar(20) not null,
    txid varchar(64) not null unique,
    copy_paste_code varchar(2048) not null,
    qr_code_base64 varchar(10000) not null,
    discord_ticket_channel_id varchar(40),
    provider_reference varchar(100),
    confirmed_at timestamptz,
    expires_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table transactions (
    id varchar(36) primary key,
    payment_id varchar(36) not null references payments(id),
    provider_event_id varchar(100) not null unique,
    amount numeric(12, 2) not null,
    payer_document varchar(40),
    paid_at timestamptz not null,
    created_at timestamptz not null
);

create table provision_rewards (
    id varchar(36) primary key,
    payment_id varchar(36) not null unique references payments(id),
    user_id varchar(36) not null references users(id),
    plan_id varchar(60) not null references vip_plans(id),
    luckperms_group varchar(80) not null,
    discord_role_id varchar(40),
    status varchar(20) not null,
    assigned_server_id varchar(100),
    external_reference varchar(100),
    failure_reason varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table audit_logs (
    id varchar(36) primary key,
    aggregate_type varchar(40) not null,
    aggregate_id varchar(100) not null,
    action varchar(80) not null,
    details varchar(1500) not null,
    created_at timestamptz not null
);

create index idx_users_minecraft_uuid on users(minecraft_uuid);
create index idx_payments_user_status on payments(user_id, status);
create index idx_transactions_payment_id on transactions(payment_id);
create index idx_rewards_status_created_at on provision_rewards(status, created_at);
create index idx_audit_logs_aggregate on audit_logs(aggregate_type, aggregate_id, created_at desc);
