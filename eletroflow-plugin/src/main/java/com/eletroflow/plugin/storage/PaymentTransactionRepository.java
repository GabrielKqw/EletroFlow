package com.eletroflow.plugin.storage;

import com.eletroflow.plugin.model.PaymentCheckResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentTransactionRepository {

    private final DatabaseManager databaseManager;

    public PaymentTransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean existsByProviderEventId(String providerEventId) {
        if (providerEventId == null || providerEventId.isBlank()) {
            return false;
        }
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1 from payment_transactions where provider_event_id = ?")) {
            statement.setString(1, providerEventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check payment transaction", exception);
        }
    }

    public void save(String paymentId, PaymentCheckResult result) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into payment_transactions (
                         transaction_id, payment_id, provider_event_id, confirmed_at, created_at
                     ) values (?, ?, ?, ?, ?)
                     """)) {
            OffsetDateTime now = OffsetDateTime.now();
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, paymentId);
            statement.setString(3, result.endToEndId());
            statement.setObject(4, result.confirmedAt());
            statement.setObject(5, now);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save payment transaction", exception);
        }
    }
}

