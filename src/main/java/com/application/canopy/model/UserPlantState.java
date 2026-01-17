package com.application.canopy.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Stato corrente di una pianta specifica dell'utente.
 */
public class UserPlantState {

    private final Plant plant;

    private boolean unlocked;
    private int totalPomodori;
    private int todayPomodori;
    private boolean dead;

    // ---- Campi extra per statistiche/achievements ----
    private LocalDate firstUseDate; // primo pomodoro completato con questa pianta
    private LocalDate lastPomodoroDate; // ultimo giorno in cui è stato fatto un pomodoro con questa pianta
    private int streakDays; // streak corrente di giorni consecutivi per questa pianta
    private int maxStreakDays; // miglior streak mai raggiunto

    public UserPlantState(Plant plant) {
        this.plant = plant;
        this.unlocked = true; // come prima (o false se volete lock iniziale)
    }

    /**
     * Costruttore usato dal GameState per ricostruire dallo storage DB.
     */
    public UserPlantState(Plant plant,
            boolean unlocked,
            int totalPomodori,
            int todayPomodori,
            boolean dead,
            LocalDate firstUseDate,
            LocalDate lastPomodoroDate,
            int streakDays,
            int maxStreakDays) {
        this.plant = plant;
        this.unlocked = unlocked;
        this.totalPomodori = totalPomodori;
        this.todayPomodori = todayPomodori;
        this.dead = dead;
        this.firstUseDate = firstUseDate;
        this.lastPomodoroDate = lastPomodoroDate;
        this.streakDays = streakDays;
        this.maxStreakDays = maxStreakDays;
    }

    // ----------------- METODI ORIGINALI -----------------

    public Plant getPlant() {
        return plant;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public int getTotalPomodori() {
        return totalPomodori;
    }

    public int getTodayPomodori() {
        return todayPomodori;
    }

    public boolean isDead() {
        return dead;
    }

    public void unlock() {
        this.unlocked = true;
    }

    /**
     * Chiamato quando un pomodoro per questa pianta viene completato.
     */
    public void onPomodoroCompleted() {
        if (dead)
            return;

        totalPomodori++;
        todayPomodori++;

        LocalDate today = LocalDate.now();

        if (firstUseDate == null) {
            firstUseDate = today;
        }

        if (lastPomodoroDate == null) {
            streakDays = 1;
        } else {
            long delta = ChronoUnit.DAYS.between(lastPomodoroDate, today);
            if (delta == 1) {
                streakDays++;
            } else if (delta > 1) {
                streakDays = 1;
            }
            // se delta == 0 -> stesso giorno, non tocchiamo la streak di giorni
        }

        lastPomodoroDate = today;
        if (streakDays > maxStreakDays) {
            maxStreakDays = streakDays;
        }
    }

    // Chiamato quando il pomodoro viene abortito -> la pianta muore
    public void onPomodoroAborted() {
        dead = true;
    }

    public void resetToday() {
        todayPomodori = 0;
    }

    // ----------------- GETTER -----------------

    public LocalDate getFirstUseDate() {
        return firstUseDate;
    }

    public LocalDate getLastPomodoroDate() {
        return lastPomodoroDate;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public int getMaxStreakDays() {
        return maxStreakDays;
    }

    // Età in giorni da quando è stato completato il primo pomodoro con questa
    // pianta.
    public int getAgeDays() {
        if (firstUseDate == null)
            return 0;
        return (int) ChronoUnit.DAYS.between(firstUseDate, LocalDate.now());
    }

    public void resetAll() {
        // Non forziamo unlocked = true, altrimenti sblocca anche le piante speciali non
        // ottenute.
        // Manteniamo lo stato attuale (o se si vuole reset totale, bisognerebbe sapere
        // se è speciale).
        // Per ora rimuoviamo la riga che forza l'unlock.
        this.totalPomodori = 0;
        this.todayPomodori = 0;
        this.dead = false;
    }

    // ----------------- SETTER -----------------

    public void setTodayPomodori(int todayPomodori) {
        this.todayPomodori = todayPomodori;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void setFirstUseDate(LocalDate firstUseDate) {
        this.firstUseDate = firstUseDate;
    }

    public void setLastPomodoroDate(LocalDate lastPomodoroDate) {
        this.lastPomodoroDate = lastPomodoroDate;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }

    public void setMaxStreakDays(int maxStreakDays) {
        this.maxStreakDays = maxStreakDays;
    }

    public void setTotalPomodori(int totalPomodori) {
        this.totalPomodori = totalPomodori;
    }
}
