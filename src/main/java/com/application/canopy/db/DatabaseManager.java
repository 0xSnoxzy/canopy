package com.application.canopy.db;

import java.nio.file.*;
import java.sql.*;

public final class DatabaseManager {

    private static Connection connection;

    private DatabaseManager() { }

    public static void init() throws SQLException {
        if (connection != null) return;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver non trovato", e);
        }

        // Posizione file DB: nella cartella utente, sottocartella .canopy
        Path baseDir = Paths.get(System.getProperty("user.home"), ".canopy");
        try {
            Files.createDirectories(baseDir);
        } catch (Exception e) {
            throw new SQLException("Impossibile creare la cartella del DB", e);
        }

        Path dbPath = baseDir.resolve("canopy.db");
        String url = "jdbc:sqlite:" + dbPath.toString();

        connection = DriverManager.getConnection(url);

        createTablesIfNeeded();
    }

    private static void createTablesIfNeeded() throws SQLException {
        // tabella attività piante
        String sql1 = """
        CREATE TABLE IF NOT EXISTS plant_activity (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            date        TEXT    NOT NULL,
            plant_name  TEXT    NOT NULL,
            minutes     INTEGER NOT NULL
        );
        """;

        try (Statement st = connection.createStatement()) {
            st.execute(sql1);
        }

        // tabella preset timer
        com.application.canopy.db.TimerPresetDao.createTableIfNeeded(connection);
        com.application.canopy.db.TimerPresetDao.ensureDefaults(connection);
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null) {
            init();
        }
        return connection;
    }

    public static void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            connection = null;
        }
    }
}
