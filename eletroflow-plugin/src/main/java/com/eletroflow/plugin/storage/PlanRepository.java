package com.eletroflow.plugin.storage;

import com.eletroflow.plugin.config.VipPlanDefinition;
import com.eletroflow.plugin.model.PlanRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlanRepository {

    private final DatabaseManager databaseManager;

    public PlanRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void sync(List<VipPlanDefinition> definitions) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            Set<String> planKeys = new HashSet<>();
            OffsetDateTime now = OffsetDateTime.now();
            for (VipPlanDefinition definition : definitions) {
                planKeys.add(definition.key());
                try (PreparedStatement statement = connection.prepareStatement("""
                        insert into vip_plans (
                            plan_key, display_name, amount, currency, luckperms_group, discord_role_id,
                            duration_days, active, sort_order, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on conflict (plan_key) do update set
                            display_name = excluded.display_name,
                            amount = excluded.amount,
                            currency = excluded.currency,
                            luckperms_group = excluded.luckperms_group,
                            discord_role_id = excluded.discord_role_id,
                            duration_days = excluded.duration_days,
                            active = excluded.active,
                            sort_order = excluded.sort_order,
                            updated_at = excluded.updated_at
                        """)) {
                    statement.setString(1, definition.key());
                    statement.setString(2, definition.displayName());
                    statement.setBigDecimal(3, definition.amount());
                    statement.setString(4, definition.currency());
                    statement.setString(5, definition.luckPermsGroup());
                    statement.setString(6, definition.discordRoleId());
                    statement.setInt(7, definition.durationDays());
                    statement.setBoolean(8, definition.active());
                    statement.setInt(9, definition.sortOrder());
                    statement.setObject(10, now);
                    statement.executeUpdate();
                }
            }
            if (planKeys.isEmpty()) {
                try (PreparedStatement statement = connection.prepareStatement("update vip_plans set active = false, updated_at = ?")) {
                    statement.setObject(1, now);
                    statement.executeUpdate();
                }
            } else {
                String placeholders = String.join(", ", java.util.Collections.nCopies(planKeys.size(), "?"));
                try (PreparedStatement statement = connection.prepareStatement(
                        "update vip_plans set active = false, updated_at = ? where plan_key not in (" + placeholders + ")")) {
                    statement.setObject(1, now);
                    int index = 2;
                    for (String planKey : planKeys) {
                        statement.setString(index++, planKey);
                    }
                    statement.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to sync VIP plans", exception);
        }
    }

    public List<PlanRecord> findActivePlans() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select plan_key, display_name, amount, currency, luckperms_group, discord_role_id, duration_days, active, sort_order
                     from vip_plans
                     where active = true
                     order by sort_order asc, amount asc
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            ArrayList<PlanRecord> records = new ArrayList<>();
            while (resultSet.next()) {
                records.add(map(resultSet));
            }
            return records;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load VIP plans", exception);
        }
    }

    public PlanRecord findRequiredPlan(String planKey) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select plan_key, display_name, amount, currency, luckperms_group, discord_role_id, duration_days, active, sort_order
                     from vip_plans
                     where plan_key = ? and active = true
                     """)) {
            statement.setString(1, planKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("VIP not found: " + planKey);
                }
                return map(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load VIP plan " + planKey, exception);
        }
    }

    private PlanRecord map(ResultSet resultSet) throws SQLException {
        return new PlanRecord(
                resultSet.getString("plan_key"),
                resultSet.getString("display_name"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("currency"),
                resultSet.getString("luckperms_group"),
                resultSet.getString("discord_role_id"),
                resultSet.getInt("duration_days"),
                resultSet.getBoolean("active"),
                resultSet.getInt("sort_order")
        );
    }
}
