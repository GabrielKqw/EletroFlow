package com.eletroflow.plugin.storage;

import com.eletroflow.plugin.model.UserRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class UserRepository {

    private final DatabaseManager databaseManager;

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public UserRecord upsert(String discordId, String minecraftUuid, String minecraftUsername) {
        try (Connection connection = databaseManager.getConnection()) {
            String existingId = findIdByDiscordId(connection, discordId);
            if (existingId == null) {
                existingId = UUID.randomUUID().toString();
                try (PreparedStatement statement = connection.prepareStatement("""
                        insert into users (user_id, discord_id, minecraft_uuid, minecraft_username, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?)
                        """)) {
                    OffsetDateTime now = OffsetDateTime.now();
                    statement.setString(1, existingId);
                    statement.setString(2, discordId);
                    statement.setString(3, minecraftUuid);
                    statement.setString(4, minecraftUsername);
                    statement.setObject(5, now);
                    statement.setObject(6, now);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("""
                        update users
                        set minecraft_uuid = ?, minecraft_username = ?, updated_at = ?
                        where user_id = ?
                        """)) {
                    statement.setString(1, minecraftUuid);
                    statement.setString(2, minecraftUsername);
                    statement.setObject(3, OffsetDateTime.now());
                    statement.setString(4, existingId);
                    statement.executeUpdate();
                }
            }
            return new UserRecord(existingId, discordId, minecraftUuid, minecraftUsername);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to upsert user", exception);
        }
    }

    private String findIdByDiscordId(Connection connection, String discordId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select user_id from users where discord_id = ?")) {
            statement.setString(1, discordId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("user_id") : null;
            }
        }
    }
}

