package com.eletroflow.plugin.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.logging.Logger;

public class SchemaMigrationService {

    private static final Logger LOGGER = Logger.getLogger(SchemaMigrationService.class.getName());
    private static final List<String> MIGRATIONS = List.of(
            "db/migration/V1__base_schema.sql",
            "db/migration/V2__remove_discord_role_columns.sql",
            "db/migration/V3__add_payment_payer_columns.sql"
    );

    private final DatabaseManager databaseManager;

    public SchemaMigrationService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void migrate() {
        Connection connection = null;
        try {
            connection = databaseManager.getConnection();
            connection.setAutoCommit(false);
            ensureVersionTable(connection);
            for (String migration : MIGRATIONS) {
                String version = versionOf(migration);
                if (isApplied(connection, version)) {
                    continue;
                }
                executeMigration(connection, migration);
                markApplied(connection, version, migration);
                LOGGER.info("Applied schema migration " + version);
            }
            connection.commit();
        } catch (SQLException exception) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Failed to run schema migrations", exception);
        } finally {
            closeQuietly(connection);
        }
    }

    private void ensureVersionTable(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                create table if not exists schema_versions (
                    version varchar(40) primary key,
                    description varchar(200) not null,
                    applied_at timestamptz not null
                )
                """)) {
            statement.executeUpdate();
        }
    }

    private boolean isApplied(Connection connection, String version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from schema_versions where version = ?")) {
            statement.setString(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void executeMigration(Connection connection, String resourcePath) throws SQLException {
        try (InputStream inputStream = SchemaMigrationService.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing migration resource " + resourcePath);
            }
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            for (String statementSql : content.split(";\\s*(?:\\r?\\n|$)")) {
                String sql = statementSql.trim();
                if (sql.isBlank()) {
                    continue;
                }
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.executeUpdate();
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read migration " + resourcePath, exception);
        }
    }

    private void markApplied(Connection connection, String version, String resourcePath) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into schema_versions (version, description, applied_at)
                values (?, ?, ?)
                """)) {
            statement.setString(1, version);
            statement.setString(2, resourcePath);
            statement.setObject(3, OffsetDateTime.now());
            statement.executeUpdate();
        }
    }

    private String versionOf(String resourcePath) {
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        int separator = fileName.indexOf("__");
        return separator < 0 ? fileName : fileName.substring(0, separator);
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
