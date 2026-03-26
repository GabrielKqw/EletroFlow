package com.eletroflow.plugin.storage;

import com.eletroflow.plugin.config.DatabaseSettings;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final DatabaseSettings settings;

    public DatabaseManager(DatabaseSettings settings) {
        this.settings = settings;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to load PostgreSQL JDBC driver", exception);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }
}
