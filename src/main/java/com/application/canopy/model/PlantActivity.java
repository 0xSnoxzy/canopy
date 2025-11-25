package com.application.canopy.model;

import java.time.LocalDate;

/**
 * Rappresenta una singola attività di cura pianta:
 * - in che giorno
 * - quale pianta
 * - quanti minuti
 */
public class PlantActivity {

    private final LocalDate date;
    private final String plantName;
    private final int minutes;

    public PlantActivity(LocalDate date, String plantName, int minutes) {
        this.date = date;
        this.plantName = plantName;
        this.minutes = minutes;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getPlantName() {
        return plantName;
    }

    public int getMinutes() {
        return minutes;
    }
}
