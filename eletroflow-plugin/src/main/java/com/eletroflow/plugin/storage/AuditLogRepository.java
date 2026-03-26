package com.eletroflow.plugin.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AuditLogRepository {

    private final DatabaseManager databaseManager;

    public AuditLogRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(String aggregateType, String aggregateId, String action, String details) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into audit_logs (
                         audit_id, aggregate_type, aggregate_id, action, details, created_at
                     ) values (?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, aggregateType);
            statement.setString(3, aggregateId);
            statement.setString(4, action);
            statement.setString(5, details);
            statement.setObject(6, OffsetDateTime.now());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save audit log", exception);
        }
    }
}
