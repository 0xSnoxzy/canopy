package com.application.canopy.model;

public class AchievementGoal {

    private final AchievementId id;
    private final String name;
    private final String shortDescription;
    private final String description;
    private final int current;
    private final int total;
    private final String iconPath; // opzionale

    public AchievementGoal(
            AchievementId id,
            String name,
            String shortDescription,
            String description,
            int current,
            int total,
            String iconPath
    ) {
        this.id = id;
        this.name = name;
        this.shortDescription = shortDescription;
        this.description = description;
        this.current = current;
        this.total = total <= 0 ? 1 : total;
        this.iconPath = iconPath;
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

    public String getIconPath() {
        return iconPath;
    }

    public double getCompletionRatio() {
        return Math.max(0.0, Math.min(1.0, (double) current / (double) total));
    }

    public boolean isCompleted() {
        return current >= total;
    }
}
