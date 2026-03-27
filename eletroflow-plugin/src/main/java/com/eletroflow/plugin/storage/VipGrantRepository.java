package com.eletroflow.plugin.storage;

import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class VipGrantRepository {

    private final DatabaseManager databaseManager;

    public VipGrantRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean existsByPaymentId(String paymentId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1 from vip_grants where payment_id = ?")) {
            statement.setString(1, paymentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check vip grant", exception);
        }
    }

    public OffsetDateTime findLatestActiveExpiry(String userId, String luckPermsGroup) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select max(expires_at) as expires_at
                     from vip_grants
                     where user_id = ?
                       and luckperms_group = ?
                       and status = ?
                       and expires_at > ?
                     """)) {
            statement.setString(1, userId);
            statement.setString(2, luckPermsGroup);
            statement.setString(3, "GRANTED");
            statement.setObject(4, OffsetDateTime.now());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getObject("expires_at", OffsetDateTime.class);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load active vip grant expiry", exception);
        }
    }

    public void saveGranted(PaymentRecord payment, PlanRecord plan, OffsetDateTime expiresAt) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into vip_grants (
                         grant_id, payment_id, user_id, plan_key, luckperms_group, granted_at, expires_at, status
                     ) values (?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            OffsetDateTime grantedAt = OffsetDateTime.now();
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, payment.id());
            statement.setString(3, payment.userId());
            statement.setString(4, plan.key());
            statement.setString(5, plan.luckPermsGroup());
            statement.setObject(6, grantedAt);
            statement.setObject(7, expiresAt);
            statement.setString(8, "GRANTED");
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save vip grant", exception);
        }
    }
}
