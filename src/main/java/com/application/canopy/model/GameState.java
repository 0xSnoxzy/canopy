package com.application.canopy.model;

import java.time.LocalDate;
import java.util.*;

public class GameState {

    private static final GameState INSTANCE = new GameState();

    private final Map<String, UserPlantState> plantStates = new HashMap<>();
    private final Map<LocalDate, String> bestPlantOfDay = new HashMap<>();

    private GameState() {
        // inizializza stati utente partendo dal catalogo
        for (Plant p : Plant.samplePlants()) {
            plantStates.put(p.getId(), new UserPlantState(p));
        }
    }

    public static GameState getInstance() {
        return INSTANCE;
    }

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

    // chiamato quando l’utente stoppa il pomodoro
    public void onPomodoroAborted(Plant plant) {
        UserPlantState state = plantStates.get(plant.getId());
        if (state != null) state.onPomodoroAborted();
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
}
