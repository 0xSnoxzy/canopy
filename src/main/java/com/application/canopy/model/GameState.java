package com.application.canopy.model;

import com.application.canopy.db.DatabaseManager;
import com.application.canopy.db.GameStateRepository;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class GameState {

    private static final GameState INSTANCE = new GameState();

    public static GameState getInstance() {
        return INSTANCE;
    }

    // Data structures per memorizzazione dello stato corrente

    private final Map<String, UserPlantState> plantStates = new HashMap<>();
    private final Map<LocalDate, String> bestPlantOfDay = new HashMap<>();

    // ----------------- STATISTICHE GLOBALI -----------------

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

    // Metodi per il tracciamento e setting della pianta corrente nella home
    private String currentPlantId;

    public String getCurrentPlantId() {
        return currentPlantId;
    }

    public void setCurrentPlantId(String id) {
        this.currentPlantId = id;
    }

    // ----------------- Collegamento al DB -----------------

    private GameStateRepository repository;

    private GameState() {
        // 1) Inizializza le plantStates dal catalogo
        for (Plant p : Plant.samplePlants()) {
            UserPlantState s = new UserPlantState(p);
            // Lock speciale per default
            if (isSpecialPlant(p.getId())) {
                s.setUnlocked(false);
            }
            plantStates.put(p.getId(), s);
        }

        // 2) Collega DB e Repository
        try {
            Connection conn = DatabaseManager.getConnection();
            repository = new GameStateRepository(conn);
            repository.initDatabase();
            loadFromRepository();
        } catch (SQLException e) {
            e.printStackTrace();
            // se il DB non è disponibile, funziona in memoria ma senza persistenza
        }

        // Controllo retroattivo (se ho già gli achievement ma la pianta era lockata)
        checkSpecialUnlocks();
    }

    private boolean isSpecialPlant(String id) {
        return "lifeblood".equals(id) || "radice_sussurrante".equals(id);
    }

    // ----------------- Caricamento dal DB -----------------

    private void loadFromRepository() {
        if (repository == null)
            return;

        // Carica stato globale
        Map<String, String> globalData = repository.loadGlobalState();
        totalPomodoriGlobal = parseInt(globalData.get("totalPomodoriGlobal"));
        globalStreak = parseInt(globalData.get("globalStreak"));
        globalBestStreak = parseInt(globalData.get("globalBestStreak"));
        lastGlobalPomodoroDate = parseDate(globalData.get("lastGlobalPomodoroDate"));
        maxPomodoriInSingleDay = parseInt(globalData.get("maxPomodoriInSingleDay"));
        hasMorningPomodoroBefore9 = parseBool(globalData.get("hasMorningPomodoroBefore9"));
        hasNightPomodoroAfter22 = parseBool(globalData.get("hasNightPomodoroAfter22"));
        totalPomodoriAfter21 = parseInt(globalData.get("totalPomodoriAfter21"));
        hasCrossedNoonPomodoro = parseBool(globalData.get("hasCrossedNoonPomodoro"));
        hasRinascitaUnlocked = parseBool(globalData.get("hasRinascitaUnlocked"));

        // Carica stato piante
        Map<String, UserPlantState> loadedPlants = repository.loadUserPlantStates();

        // Merge: se nel DB c'è lo stato, usa quello (che avrà unlocked=true/false
        // salvato),
        // altrimenti mantieni il default (che per le speciali è false)
        for (Map.Entry<String, UserPlantState> entry : loadedPlants.entrySet()) {
            plantStates.put(entry.getKey(), entry.getValue());
        }
    }

    // ----------------- LOGICA -----------------

    public Collection<UserPlantState> getAllPlantStates() {
        return plantStates.values();
    }

    private void checkSpecialUnlocks() {
        // Achievement MATTINIERO -> Lifeblood
        if (plantStates.containsKey("lifeblood")) {
            if (hasMorningPomodoroBefore9) {
                unlockPlantIfLocked("lifeblood");
            } else {
                lockPlantIfUnlocked("lifeblood");
            }
        }

        // Achievement FOGLIA NUOVA -> Radice Sussurrante
        if (plantStates.containsKey("radice_sussurrante")) {
            if (maxPomodoriInSingleDay >= 3) {
                unlockPlantIfLocked("radice_sussurrante");
            } else {
                lockPlantIfUnlocked("radice_sussurrante");
            }
        }
    }

    private void unlockPlantIfLocked(String plantId) {
        UserPlantState s = plantStates.get(plantId);
        if (s != null && !s.isUnlocked()) {
            s.unlock();
            if (repository != null) {
                repository.saveUserPlantState(s);
            }
        }
    }

    private void lockPlantIfUnlocked(String plantId) {
        UserPlantState s = plantStates.get(plantId);
        if (s != null && s.isUnlocked()) {
            s.setUnlocked(false);
            if (repository != null) {
                repository.saveUserPlantState(s);
            }
        }
    }

    public List<Plant> getAllPlants() {
        return plantStates.values().stream().map(UserPlantState::getPlant).toList();
    }

    public UserPlantState getStateFor(Plant plant) {
        return plantStates.get(plant.getId());
    }

    // chiamato quando un pomodoro finisce senza essere interrotto
    public void onPomodoroCompleted(Plant plant) {
        UserPlantState state = plantStates.get(plant.getId());
        if (state == null)
            return;

        state.onPomodoroCompleted();

        LocalDate today = LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();

        // ---- MIGLIOR PIANTA DEL GIORNO ----
        updateBestPlantOfDay(today, plant, state);

        // ---- STATISTICHE GLOBALI PER ACHIEVEMENTS ----
        updateGlobalStats(today, nowTime);

        // Check unlock speciali
        checkSpecialUnlocks();

        // Salva su DB
        if (repository != null) {
            saveGlobalState();
            repository.saveUserPlantState(state);
        }
    }

    private void updateBestPlantOfDay(LocalDate today, Plant plant, UserPlantState state) {
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
    }

    private void updateGlobalStats(LocalDate today, java.time.LocalTime nowTime) {
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
    }

    // chiamato quando l’utente stoppa il pomodoro
    public void onPomodoroAborted(Plant plant) {
        UserPlantState state = plantStates.get(plant.getId());
        if (state != null) {
            state.onPomodoroAborted();
            if (repository != null) {
                repository.saveUserPlantState(state);
            }
        }
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
            entry.getValue().resetAll();
            // N.B. resetAll() deve esistere in UserPlantState, l'abbiamo visto prima.
            // Se non fa esattamente quello che vogliamo, potremmo doverlo aggiornare.
            // Nel codice originale ricreava l'oggetto, qui resetto quello esistente.
        }

        // 4) sincronizza sul DB
        if (repository != null) {
            repository.clearAllData();
            // salva stato globale azzerato (che ora è vuoto/zero)
            saveGlobalState();
            // salva stati per pianta azzerati
            for (UserPlantState s : plantStates.values()) {
                repository.saveUserPlantState(s);
            }
        }
    }

    // ----------------- LISTENERS -----------------
    private final List<Runnable> statsListeners = new ArrayList<>();

    public void addStatsListener(Runnable r) {
        if (r != null)
            statsListeners.add(r);
    }

    private void fireStatsChanged() {
        for (Runnable r : statsListeners) {
            try {
                r.run();
            } catch (Exception ignore) {
            }
        }
    }

    private void saveGlobalState() {
        if (repository == null)
            return;
        repository.saveGlobalKey("totalPomodoriGlobal", String.valueOf(totalPomodoriGlobal));
        repository.saveGlobalKey("globalStreak", String.valueOf(globalStreak));
        repository.saveGlobalKey("globalBestStreak", String.valueOf(globalBestStreak));
        repository.saveGlobalKey("lastGlobalPomodoroDate", formatDate(lastGlobalPomodoroDate));
        repository.saveGlobalKey("maxPomodoriInSingleDay", String.valueOf(maxPomodoriInSingleDay));
        repository.saveGlobalKey("hasMorningPomodoroBefore9", boolToString(hasMorningPomodoroBefore9));
        repository.saveGlobalKey("hasNightPomodoroAfter22", boolToString(hasNightPomodoroAfter22));
        repository.saveGlobalKey("totalPomodoriAfter21", String.valueOf(totalPomodoriAfter21));
        repository.saveGlobalKey("hasCrossedNoonPomodoro", boolToString(hasCrossedNoonPomodoro));
        repository.saveGlobalKey("hasRinascitaUnlocked", boolToString(hasRinascitaUnlocked));

        fireStatsChanged();
    }

    // ----------------- GETTER PER ACHIEVEMENTS -----------------

    public Plant getBestPlantOf(LocalDate date) {
        String id = bestPlantOfDay.get(date);
        if (id == null)
            return null;
        UserPlantState s = plantStates.get(id);
        return s != null ? s.getPlant() : null;
    }

    public Map<LocalDate, String> getBestPlantsOfDayRaw() {
        return Collections.unmodifiableMap(bestPlantOfDay);
    }

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

    // ----------------- UTILITY PARSING -----------------

    private int parseInt(String s) {
        if (s == null)
            return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
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

    private boolean parseBool(String s) {
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    private String boolToString(boolean b) {
        return b ? "1" : "0";
    }
}
