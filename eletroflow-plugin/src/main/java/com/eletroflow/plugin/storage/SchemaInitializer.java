package com.eletroflow.plugin.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {

    private final DatabaseManager databaseManager;

    public SchemaInitializer(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void initialize() {
        try (Connection connection = databaseManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
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
                    )
                    """);
            statement.execute("""
                    create table if not exists payments (
                        payment_id varchar(36) primary key,
                        discord_id varchar(40) not null,
                        minecraft_uuid varchar(36) not null,
                        minecraft_username varchar(16) not null,
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
                    )
                    """);
            statement.execute("create index if not exists idx_payments_status_created_at on payments(status, created_at)");
            statement.execute("create index if not exists idx_payments_discord_id on payments(discord_id)");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize PostgreSQL schema", exception);
        }
    }
}

