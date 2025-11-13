package com.application.canopy.model;

public class UserPlantState {

    private final Plant plant;
    private boolean unlocked;
    private int totalPomodori;
    private int todayPomodori;
    private boolean dead;

    public UserPlantState(Plant plant) {
        this.plant = plant;
        this.unlocked = true; // oppure false se vuoi lock iniziale
    }

    public Plant getPlant() { return plant; }
    public boolean isUnlocked() { return unlocked; }
    public int getTotalPomodori() { return totalPomodori; }
    public int getTodayPomodori() { return todayPomodori; }
    public boolean isDead() { return dead; }

    public void unlock() { this.unlocked = true; }

    public void onPomodoroCompleted() {
        if (dead) return;
        totalPomodori++;
        todayPomodori++;
    }

    public void onPomodoroAborted() {
        // se vuoi essere cattivo, la pianta muore
        dead = true;
    }

    public void resetToday() {
        todayPomodori = 0;
    }
}
