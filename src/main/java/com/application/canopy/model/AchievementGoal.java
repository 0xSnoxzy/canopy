package com.application.canopy.model;

public class AchievementGoal {
    //final? Una volta assegnato nel costruttore, non cambia più, niente setters
    private final AchievementId id;
    private final String name;
    private final String shortDescription;
    private final String description;
    private final int current;
    private final int total;

    //costruttore
    public AchievementGoal(
            AchievementId id,
            String name,
            String shortDescription,
            String description,
            int current,
            int total
    ) {
        //si mette this per differenziare la variablile del oggeto dal parametro
        this.id = id;
        this.name = name;
        this.shortDescription = shortDescription;
        this.description = description;
        this.current = current;
        this.total = total <= 0 ? 1 : total;//evitiamo divisione per zero
    }

    public AchievementId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public int getCurrent() {
        return current;
    }

    public int getTotal() {
        return total;
    }

    public double getCompletionRatio() {//Calcola una percentuale da 0.0 a 1.0 senza sorpassare quel limite 0.0 e 1.0
        return Math.max(0.0, Math.min(1.0, (double) current / (double) total));
    }

    public boolean isCompleted() {
        return current >= total;
    }
}
