package com.application.canopy.model;

import com.application.canopy.db.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Stato principale del gioco.
 * - Mantiene compatibilità con i metodi esistenti (home, ecc.)
 * - Esposto per AchievementManager
 * - Persistente tramite SQLite (via DatabaseManager)
 */
public class GameState {

    private static final GameState INSTANCE = new GameState();

    public static GameState getInstance() {
        return INSTANCE;
    }

    // ----------------- STORAGE IN MEMORIA -----------------

    private final Map<String, UserPlantState> plantStates = new HashMap<>();
    private final Map<LocalDate, String> bestPlantOfDay = new HashMap<>();

    // ----------------- STATISTICHE GLOBALI PER ACHIEVEMENTS -----------------

    private int totalPomodoriGlobal;
    private int globalStreak;
    private int globalBestStreak;
    private LocalDate lastGlobalPomodoroDate;

    private final Map<LocalDate, Integer> completedPomodoriPerDay = new HashMap<>();
    private int maxPomodoriInSingleDay;

    private boolean hasMorningPomodoroBefore9;
    private boolean hasNightPomodoroAfter22;
    private int totalPomodoriAfter21;
    private boolean hasCrossedNoonPomodoro;
    private boolean hasRinascitaUnlocked;

    // ----------------- DB -----------------

    private Connection connection;

    private static final String TABLE_GAME_STATE = "game_state";
    private static final String TABLE_USER_PLANT_STATE = "user_plant_state";

    private GameState() {
        // 1) Inizializza le plantStates dal catalogo
        for (Plant p : Plant.samplePlants()) {
            plantStates.put(p.getId(), new UserPlantState(p));
        }

        // 2) Collega DB e prepara tabelle
        try {
            connection = DatabaseManager.getConnection();
            initDatabase();
            loadFromDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            // se il DB non è disponibile, l'app funziona lo stesso ma senza persistenza
        }
    }

    // ----------------- INIZIALIZZAZIONE DB -----------------

    private void initDatabase() throws SQLException {
        if (connection == null) return;

        try (Statement st = connection.createStatement()) {
            // key/value globale
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_GAME_STATE + " (" +
                            "key TEXT PRIMARY KEY," +
                            "value TEXT NOT NULL" +
                            ");"
            );

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
                            ");"
            );
        }
    }

    // ----------------- CARICAMENTO DAL DB -----------------

    private void loadFromDatabase() {
        if (connection == null) return;

        loadGlobalStateFromDb();
        loadUserPlantStatesFromDb();
    }



    private void loadGlobalStateFromDb() {
        String sql = "SELECT key, value FROM " + TABLE_GAME_STATE;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String key = rs.getString("key");
                String value = rs.getString("value");

                switch (key) {
                    case "totalPomodoriGlobal" -> totalPomodoriGlobal = parseInt(value);
                    case "globalStreak" -> globalStreak = parseInt(value);
                    case "globalBestStreak" -> globalBestStreak = parseInt(value);
                    case "lastGlobalPomodoroDate" -> lastGlobalPomodoroDate = parseDate(value);
                    case "maxPomodoriInSingleDay" -> maxPomodoriInSingleDay = parseInt(value);
                    case "hasMorningPomodoroBefore9" -> hasMorningPomodoroBefore9 = parseBool(value);
                    case "hasNightPomodoroAfter22" -> hasNightPomodoroAfter22 = parseBool(value);
                    case "totalPomodoriAfter21" -> totalPomodoriAfter21 = parseInt(value);
                    case "hasCrossedNoonPomodoro" -> hasCrossedNoonPomodoro = parseBool(value);
                    case "hasRinascitaUnlocked" -> hasRinascitaUnlocked = parseBool(value);
                    default -> {
                        // ignora chiavi sconosciute (forward compatibility)
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadUserPlantStatesFromDb() {
        if (connection == null) return;

        String sql = "SELECT * FROM " + TABLE_USER_PLANT_STATE;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String plantId = rs.getString("plant_id");
                Plant plant = findPlantById(plantId);
                if (plant == null) continue;

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
                        maxStreakDays
                );
                plantStates.put(plantId, ups);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private Plant findPlantById(String id) {
        if (id == null) return null;
        for (Plant p : Plant.samplePlants()) {
            if (id.equals(p.getId())) return p;
        }
        return null;
    }

    // ----------------- SALVATAGGIO SU DB -----------------

    private void saveGlobalStateToDb() {
        if (connection == null) return;

        String sql = "INSERT OR REPLACE INTO " + TABLE_GAME_STATE + " (key, value) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            saveKeyValue(ps, "totalPomodoriGlobal", String.valueOf(totalPomodoriGlobal));
            saveKeyValue(ps, "globalStreak", String.valueOf(globalStreak));
            saveKeyValue(ps, "globalBestStreak", String.valueOf(globalBestStreak));
            saveKeyValue(ps, "lastGlobalPomodoroDate", formatDate(lastGlobalPomodoroDate));
            saveKeyValue(ps, "maxPomodoriInSingleDay", String.valueOf(maxPomodoriInSingleDay));
            saveKeyValue(ps, "hasMorningPomodoroBefore9", boolToString(hasMorningPomodoroBefore9));
            saveKeyValue(ps, "hasNightPomodoroAfter22", boolToString(hasNightPomodoroAfter22));
            saveKeyValue(ps, "totalPomodoriAfter21", String.valueOf(totalPomodoriAfter21));
            saveKeyValue(ps, "hasCrossedNoonPomodoro", boolToString(hasCrossedNoonPomodoro));
            saveKeyValue(ps, "hasRinascitaUnlocked", boolToString(hasRinascitaUnlocked));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveKeyValue(PreparedStatement ps, String key, String value) throws SQLException {
        if (value == null) {
            value = ""; // oppure "null", come preferisci, ma NON null
        }
        ps.setString(1, key);
        ps.setString(2, value);
        ps.executeUpdate();
    }

    private void saveUserPlantStateToDb(UserPlantState s) {
        if (connection == null || s == null) return;

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

    // ----------------- METODI ORIGINALI PUBBLICI -----------------

    public Collection<UserPlantState> getAllPlantStates() {
        return plantStates.values();
    }

    public List<Plant> getAllPlants() {
        return plantStates.values().stream()
                .map(UserPlantState::getPlant)
                .toList();
    }

    public UserPlantState getStateFor(Plant plant) {
        return plantStates.get(plant.getId());
    }

    // chiamato quando un pomodoro finisce senza essere interrotto
    public void onPomodoroCompleted(Plant plant) {
        UserPlantState state = plantStates.get(plant.getId());
        if (state == null) return;

        state.onPomodoroCompleted();

        LocalDate today = LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();

        // ---- MIGLIOR PIANTA DEL GIORNO (logica originale) ----
        String currentBestId = bestPlantOfDay.get(today);
        if (currentBestId == null) {
            bestPlantOfDay.put(today, plant.getId());
        } else {
            UserPlantState currentBest = plantStates.get(currentBestId);
            if (currentBest == null ||
                    state.getTodayPomodori() > currentBest.getTodayPomodori()) {
                bestPlantOfDay.put(today, plant.getId());
            }
        }

        // ---- STATISTICHE GLOBALI PER ACHIEVEMENTS ----

        totalPomodoriGlobal++;

        if (lastGlobalPomodoroDate == null) {
            globalStreak = 1;
        } else {
            long delta = ChronoUnit.DAYS.between(lastGlobalPomodoroDate, today);

            if (delta >= 7) {
                hasRinascitaUnlocked = true;
                globalStreak = 1;
            } else if (delta == 1) {
                globalStreak++;
            } else if (delta > 1) {
                globalStreak = 1;
            }
        }
        lastGlobalPomodoroDate = today;

        if (globalStreak > globalBestStreak) {
            globalBestStreak = globalStreak;
        }

        int dayCount = completedPomodoriPerDay.getOrDefault(today, 0) + 1;
        completedPomodoriPerDay.put(today, dayCount);

        if (dayCount > maxPomodoriInSingleDay) {
            maxPomodoriInSingleDay = dayCount;
        }

        if (nowTime.isBefore(java.time.LocalTime.of(9, 0))) {
            hasMorningPomodoroBefore9 = true;
        }

        if (nowTime.isAfter(java.time.LocalTime.of(22, 0))) {
            hasNightPomodoroAfter22 = true;
        }

        if (nowTime.isAfter(java.time.LocalTime.of(21, 0))) {
            totalPomodoriAfter21++;
        }

        java.time.LocalTime startApprox = nowTime.minusMinutes(25); // assume 25min per pomodoro
        java.time.LocalTime noon = java.time.LocalTime.NOON;
        if (!startApprox.isAfter(noon) && !nowTime.isBefore(noon)) {
            hasCrossedNoonPomodoro = true;
        }

        // salva su DB lo stato globale + quello della pianta
        saveGlobalStateToDb();
        saveUserPlantStateToDb(state);
    }

    // chiamato quando l’utente stoppa il pomodoro
    public void onPomodoroAborted(Plant plant) {
        UserPlantState state = plantStates.get(plant.getId());
        if (state != null) {
            state.onPomodoroAborted();
            saveUserPlantStateToDb(state);
        }
    }

    public Plant getBestPlantOf(LocalDate date) {
        String id = bestPlantOfDay.get(date);
        if (id == null) return null;
        UserPlantState s = plantStates.get(id);
        return s != null ? s.getPlant() : null;
    }

    public Map<LocalDate, String> getBestPlantsOfDayRaw() {
        return Collections.unmodifiableMap(bestPlantOfDay);
    }

    // ----------------- GETTER PER ACHIEVEMENTS -----------------

    public int getTotalPomodoriGlobal() {
        return totalPomodoriGlobal;
    }

    public int getGlobalStreak() {
        return globalStreak;
    }

    public int getGlobalBestStreak() {
        return globalBestStreak;
    }

    public LocalDate getLastGlobalPomodoroDate() {
        return lastGlobalPomodoroDate;
    }

    public int getMaxPomodoriInSingleDay() {
        return maxPomodoriInSingleDay;
    }

    public boolean hasMorningPomodoroBefore9() {
        return hasMorningPomodoroBefore9;
    }

    public boolean hasNightPomodoroAfter22() {
        return hasNightPomodoroAfter22;
    }

    public int getTotalPomodoriAfter21() {
        return totalPomodoriAfter21;
    }

    public boolean hasCrossedNoonPomodoro() {
        return hasCrossedNoonPomodoro;
    }

    public boolean hasRinascitaUnlocked() {
        return hasRinascitaUnlocked;
    }

    public void resetAllProgress() {
        // 1) reset statistiche globali in memoria
        totalPomodoriGlobal = 0;
        globalStreak = 0;
        globalBestStreak = 0;
        lastGlobalPomodoroDate = null;

        completedPomodoriPerDay.clear();
        maxPomodoriInSingleDay = 0;

        hasMorningPomodoroBefore9 = false;
        hasNightPomodoroAfter22 = false;
        totalPomodoriAfter21 = 0;
        hasCrossedNoonPomodoro = false;
        hasRinascitaUnlocked = false;

        // 2) reset best plant of day
        bestPlantOfDay.clear();

        // 3) ricrea gli UserPlantState azzerando i progressi ma mantenendo l'unlock
        for (Map.Entry<String, UserPlantState> entry : plantStates.entrySet()) {
            UserPlantState old = entry.getValue();
            Plant plant = old.getPlant();
            boolean unlocked = old.isUnlocked();

            UserPlantState reset = new UserPlantState(
                    plant,
                    unlocked,
                    0,          // totalPomodori
                    0,          // todayPomodori
                    false,      // dead
                    null,       // firstUseDate
                    null,       // lastPomodoroDate
                    0,          // streakDays
                    0           // maxStreakDays
            );
            entry.setValue(reset);
        }

        // 4) sincronizza sul DB: svuota le tabelle e risalva lo stato vuoto
        if (connection != null) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("DELETE FROM " + TABLE_GAME_STATE);
                st.executeUpdate("DELETE FROM " + TABLE_USER_PLANT_STATE);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // salva stato globale azzerato
            saveGlobalStateToDb();
            // salva stati per pianta azzerati
            for (UserPlantState s : plantStates.values()) {
                saveUserPlantStateToDb(s);
            }
        }
    }

    // ----------------- UTILITY PARSING -----------------

    private int parseInt(String s) {
        if (s == null) return 0;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return LocalDate.parse(s); }
        catch (Exception e) { return null; }
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private boolean parseBool(String s) {
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    private String boolToString(boolean b) {
        return b ? "1" : "0";
    }


}
