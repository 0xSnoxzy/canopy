package com.application.canopy.model;

import java.util.List;

public class Plant {
    private final String name;
    private final String description;
    private final String category;

    public Plant(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }

    // mock data
    public static List<Plant> samplePlants() {
        return List.of(
                new Plant("Lavanda", "Pianta aromatica dal profumo intenso, usata per tisane e profumi.", "Erbe"),
                new Plant("Quercia", "Albero longevo e maestoso, simbolo di forza.", "Alberi"),
                new Plant("Rosa", "Fiore elegante e profumato, molte varietà e colori.", "Fiori"),
                new Plant("Menta", "Erba fresca usata in cucina e bevande.", "Erbe"),
                new Plant("Acero", "Foglie palmate, colori autunnali intensi.", "Alberi"),
                new Plant("Girasole", "Fiore che segue il sole, semi oleosi.", "Fiori")
        );
    }
}
