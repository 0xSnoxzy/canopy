package com.application.canopy.db;

import com.application.canopy.model.TimerPreset;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TimerPresetDao {

    public static void createTableIfNeeded(Connection conn) throws SQLException {
        // schema completo con repeat_before_long
        String sql = """
            CREATE TABLE IF NOT EXISTS timer_preset (
                id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                name                TEXT NOT NULL UNIQUE,
                focus_minutes       INTEGER NOT NULL,
                short_break         INTEGER NOT NULL,
                long_break          INTEGER NOT NULL,
                repeat_before_long  INTEGER NOT NULL
            );
            """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }

        // migrazione: se la colonna non c'è (vecchi DB), la aggiungiamo
        try (Statement st = conn.createStatement()) {
            st.execute("""
                ALTER TABLE timer_preset
                ADD COLUMN repeat_before_long INTEGER NOT NULL DEFAULT 3
            """);
        } catch (SQLException e) {
            // se la colonna esiste già → ignoriamo
        }
    }

    public static List<TimerPreset> findAll(Connection conn) throws SQLException {
        String sql = """
            SELECT id, name, focus_minutes, short_break, long_break, repeat_before_long
            FROM timer_preset
            ORDER BY id
            """;
        List<TimerPreset> list = new ArrayList<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new TimerPreset(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("focus_minutes"),
                        rs.getInt("short_break"),
                        rs.getInt("long_break"),
                        rs.getInt("repeat_before_long")
                ));
            }
        }
        return list;
    }

    public static TimerPreset insert(Connection conn, TimerPreset p) throws SQLException {
        String sql = """
            INSERT INTO timer_preset
                (name, focus_minutes, short_break, long_break, repeat_before_long)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getFocusMinutes());
            ps.setInt(3, p.getShortBreakMinutes());
            ps.setInt(4, p.getLongBreakMinutes());
            ps.setInt(5, p.getRepeatBeforeLongBreak());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getInt(1));
                }
            }
        }
        return p;
    }

    public static void update(Connection conn, TimerPreset p) throws SQLException {
        if (p.getId() <= 0) throw new IllegalArgumentException("Preset senza id");
        String sql = """
            UPDATE timer_preset
            SET name = ?,
                focus_minutes = ?,
                short_break = ?,
                long_break = ?,
                repeat_before_long = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getFocusMinutes());
            ps.setInt(3, p.getShortBreakMinutes());
            ps.setInt(4, p.getLongBreakMinutes());
            ps.setInt(5, p.getRepeatBeforeLongBreak());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        }
    }

    public static void delete(Connection conn, TimerPreset p) throws SQLException {
        if (p.getId() <= 0) return;
        String sql = "DELETE FROM timer_preset WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            ps.executeUpdate();
        }
    }

    public static void ensureDefaults(Connection conn) throws SQLException {
        List<TimerPreset> all = findAll(conn);
        if (!all.isEmpty()) return;

        // Pomodoro classico: 3 focus → long break
        insert(conn, new TimerPreset("Pomodoro", 25, 5, 15, 3));
        // esempio: 2 focus → long break
        insert(conn, new TimerPreset("Concentrazione profonda", 60, 10, 20, 2));
    }
}
