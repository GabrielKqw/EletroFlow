package com.eletroflow.plugin.storage;

import com.eletroflow.plugin.model.PaymentCheckResult;
import com.eletroflow.plugin.model.PaymentCreation;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.shared.enums.PaymentStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentRepository {

    private final DatabaseManager databaseManager;

    public PaymentRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<PaymentRecord> findReusablePendingPayment(String discordId, String planKey) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select p.*, u.discord_id, u.minecraft_uuid, u.minecraft_username
                     from payments p
                     join users u on u.user_id = p.user_id
                     where u.discord_id = ? and p.plan_key = ? and p.status = ?
                     order by created_at desc
                     limit 1
                     """)) {
            statement.setString(1, discordId);
            statement.setString(2, planKey);
            statement.setString(3, PaymentStatus.PENDING.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                PaymentRecord record = map(resultSet);
                if (record.expiresAt().isBefore(OffsetDateTime.now())) {
                    return Optional.empty();
                }
                return Optional.of(record);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load pending payment", exception);
        }
    }

    public void savePayment(PaymentCreation creation) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into payments (
                         payment_id, user_id, plan_key, amount, txid, copy_paste_code, qr_code_base64, discord_thread_id, provider_reference,
                         status, created_at, expires_at
                     ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, creation.id());
            statement.setString(2, creation.userId());
            statement.setString(3, creation.planKey());
            statement.setBigDecimal(4, creation.amount());
            statement.setString(5, creation.txid());
            statement.setString(6, creation.copyPasteCode());
            statement.setString(7, creation.qrCodeBase64());
            statement.setString(8, creation.discordThreadId());
            statement.setString(9, creation.txid());
            statement.setString(10, PaymentStatus.PENDING.name());
            statement.setObject(11, OffsetDateTime.now());
            statement.setObject(12, creation.expiresAt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save payment", exception);
        }
    }

    public List<PaymentRecord> findPendingPayments() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select p.*, u.discord_id, u.minecraft_uuid, u.minecraft_username
                     from payments p
                     join users u on u.user_id = p.user_id
                     where p.status = ?
                       and p.rewarded_at is null
                       and p.expires_at > ?
                     order by p.created_at asc
                     """)) {
            statement.setString(1, PaymentStatus.PENDING.name());
            statement.setObject(2, OffsetDateTime.now());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PaymentRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(map(resultSet));
                }
                return records;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list pending payments", exception);
        }
    }

    public void markConfirmed(String paymentId, PaymentCheckResult result) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     update payments
                     set status = ?, confirmed_at = ?, provider_event_id = ?
                     where payment_id = ? and status <> ?
                     """)) {
            statement.setString(1, PaymentStatus.CONFIRMED.name());
            statement.setObject(2, result.confirmedAt());
            statement.setString(3, result.endToEndId());
            statement.setString(4, paymentId);
            statement.setString(5, PaymentStatus.CONFIRMED.name());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to confirm payment", exception);
        }
    }

    public void markRewarded(String paymentId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     update payments
                     set rewarded_at = ?
                     where payment_id = ?
                     """)) {
            statement.setObject(1, OffsetDateTime.now());
            statement.setString(2, paymentId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark rewarded payment", exception);
        }
    }

    private PaymentRecord map(ResultSet resultSet) throws SQLException {
        return new PaymentRecord(
                resultSet.getString("payment_id"),
                resultSet.getString("user_id"),
                resultSet.getString("discord_id"),
                resultSet.getString("minecraft_uuid"),
                resultSet.getString("minecraft_username"),
                resultSet.getString("plan_key"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("txid"),
                resultSet.getString("copy_paste_code"),
                resultSet.getString("qr_code_base64"),
                resultSet.getString("discord_thread_id"),
                PaymentStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("expires_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("confirmed_at", OffsetDateTime.class),
                resultSet.getObject("rewarded_at", OffsetDateTime.class)
        );
    }
}
