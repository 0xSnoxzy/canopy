package com.application.canopy.db;

import com.application.canopy.model.PlantActivity;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PlantActivityRepository {

    private final Connection connection;

    public PlantActivityRepository(Connection connection) {
        this.connection = connection;
    }

    public void addActivity(LocalDate date, String plantName, int minutes) throws SQLException {
        String sql = "INSERT INTO plant_activity (date, plant_name, minutes) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setString(2, plantName);
            ps.setInt(3, minutes);
            ps.executeUpdate();
        }
    }

    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM plant_activity";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    // Getter di tutte le attività tra due date (estremi inclusi)
    public List<PlantActivity> getActivitiesBetween(LocalDate from, LocalDate to) throws SQLException {
        String sql = """
                SELECT date, plant_name, minutes
                FROM plant_activity
                WHERE date BETWEEN ? AND ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<PlantActivity> list = new ArrayList<>();
                while (rs.next()) {
                    LocalDate d = LocalDate.parse(rs.getString("date"));
                    String name = rs.getString("plant_name");
                    int min = rs.getInt("minutes");
                    list.add(new PlantActivity(d, name, min));
                }
                return list;
            }
        }
    }

    // Getter di tutte le attività di un singolo giorno
    public List<PlantActivity> getActivitiesForDate(LocalDate date) throws SQLException {
        String sql = "SELECT plant_name, minutes FROM plant_activity WHERE date = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<PlantActivity> list = new ArrayList<>();
                while (rs.next()) {
                    String name = rs.getString("plant_name");
                    int min = rs.getInt("minutes");
                    list.add(new PlantActivity(date, name, min));
                }
                return list;
            }
        }
    }
}
