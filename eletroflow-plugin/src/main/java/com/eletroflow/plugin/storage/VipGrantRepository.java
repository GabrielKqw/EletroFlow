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

    public void saveGranted(PaymentRecord payment, PlanRecord plan) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into vip_grants (
                         grant_id, payment_id, user_id, plan_key, luckperms_group, discord_role_id,
                         granted_at, expires_at, status
                     ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            OffsetDateTime grantedAt = OffsetDateTime.now();
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, payment.id());
            statement.setString(3, payment.userId());
            statement.setString(4, plan.key());
            statement.setString(5, plan.luckPermsGroup());
            statement.setString(6, plan.discordRoleId());
            statement.setObject(7, grantedAt);
            statement.setObject(8, grantedAt.plusDays(plan.durationDays()));
            statement.setString(9, "GRANTED");
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save vip grant", exception);
        }
    }
}

