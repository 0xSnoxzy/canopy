package com.application.canopy.db;

import com.application.canopy.model.Plant;
import com.application.canopy.model.UserPlantState;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class GameStateRepository {

    private final Connection connection;

    private static final String TABLE_GAME_STATE = "game_state";
    private static final String TABLE_USER_PLANT_STATE = "user_plant_state";

    public GameStateRepository(Connection connection) {
        this.connection = connection;
    }

    public void initDatabase() throws SQLException {
        if (connection == null)
            return;

        try (Statement st = connection.createStatement()) {
            // key/value globale
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_GAME_STATE + " (" +
                            "key TEXT PRIMARY KEY," +
                            "value TEXT NOT NULL" +
                            ");");

            // stato per pianta
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_USER_PLANT_STATE + " (" +
                            "plant_id TEXT PRIMARY KEY," +
                            "unlocked INTEGER NOT NULL," +
                            "total_pomodori INTEGER NOT NULL," +
                            "today_pomodori INTEGER NOT NULL," +
                            "dead INTEGER NOT NULL," +
                            "first_use_date TEXT," +
                            "last_pomodoro_date TEXT," +
                            "streak_days INTEGER NOT NULL," +
                            "max_streak_days INTEGER NOT NULL" +
                            ");");
        }
    }

    // Caricamento dei dati nuovi dal DB
    public Map<String, String> loadGlobalState() {
        Map<String, String> data = new HashMap<>();
        if (connection == null)
            return data;

        String sql = "SELECT key, value FROM " + TABLE_GAME_STATE;
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String key = rs.getString("key");
                String value = rs.getString("value");
                data.put(key, value);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public Map<String, UserPlantState> loadUserPlantStates() {
        Map<String, UserPlantState> result = new HashMap<>();
        if (connection == null)
            return result;

        String sql = "SELECT * FROM " + TABLE_USER_PLANT_STATE;
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String plantId = rs.getString("plant_id");
                Plant plant = findPlantById(plantId);
                if (plant == null)
                    continue;

                boolean unlocked = rs.getInt("unlocked") != 0;
                int totalPomodori = rs.getInt("total_pomodori");
                int todayPomodori = rs.getInt("today_pomodori");
                boolean dead = rs.getInt("dead") != 0;
                LocalDate firstUseDate = parseDate(rs.getString("first_use_date"));
                LocalDate lastPomodoroDate = parseDate(rs.getString("last_pomodoro_date"));
                int streakDays = rs.getInt("streak_days");
                int maxStreakDays = rs.getInt("max_streak_days");

                UserPlantState ups = new UserPlantState(
                        plant,
                        unlocked,
                        totalPomodori,
                        todayPomodori,
                        dead,
                        firstUseDate,
                        lastPomodoroDate,
                        streakDays,
                        maxStreakDays);
                result.put(plantId, ups);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void clearAllData() {
        if (connection == null)
            return;
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM " + TABLE_GAME_STATE);
            st.executeUpdate("DELETE FROM " + TABLE_USER_PLANT_STATE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Salvataggio dei dati nuovi sul DB

    public void saveGlobalKey(String key, String value) {
        if (connection == null)
            return;
        String sql = "INSERT OR REPLACE INTO " + TABLE_GAME_STATE + " (key, value) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value == null ? "" : value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveUserPlantState(UserPlantState s) {
        if (connection == null || s == null)
            return;

        String sql = "INSERT OR REPLACE INTO " + TABLE_USER_PLANT_STATE + " (" +
                "plant_id, unlocked, total_pomodori, today_pomodori, dead," +
                " first_use_date, last_pomodoro_date, streak_days, max_streak_days" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s.getPlant().getId());
            ps.setInt(2, s.isUnlocked() ? 1 : 0);
            ps.setInt(3, s.getTotalPomodori());
            ps.setInt(4, s.getTodayPomodori());
            ps.setInt(5, s.isDead() ? 1 : 0);
            ps.setString(6, formatDate(s.getFirstUseDate()));
            ps.setString(7, formatDate(s.getLastPomodoroDate()));
            ps.setInt(8, s.getStreakDays());
            ps.setInt(9, s.getMaxStreakDays());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Metodi utili
    private Plant findPlantById(String id) {
        if (id == null)
            return null;
        for (Plant p : Plant.samplePlants()) {
            if (id.equals(p.getId()))
                return p;
        }
        return null;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isEmpty())
            return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
