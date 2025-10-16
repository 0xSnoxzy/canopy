package com.application.canopy.model;

import java.util.List;

public class Plant {
    private final String name;
    private final String latinName;
    private final String description;
    private final String category;

    public Plant(String name, String latinName, String description, String category) {
        this.name = name;
        this.latinName = latinName;
        this.description = description;
        this.category = category;
    }

    public String getName() { return name; }
    public String getLatinName() { return latinName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }

    // mock data
    public static List<Plant> samplePlants() {
        return List.of(
                new Plant("Lavanda", "Lavandula angustifolia", "Pianta aromatica dal profumo intenso, usata per tisane e profumi.", "Erbe"),
                new Plant("Quercia", "Quercus robur", "Albero longevo e maestoso, simbolo di forza.", "Alberi"),
                new Plant("Rosa", "Rosa spp.", "Fiore elegante e profumato, molte varietà e colori.", "Fiori"),
                new Plant("Menta", "Mentha ", "Erba fresca usata in cucina e bevande.", "Erbe"),
                new Plant("Acero", "Acer", "Foglie palmate, colori autunnali intensi.", "Alberi"),
                new Plant("Girasole", "Helianthus annuus", "Fiore che segue il sole, semi oleosi.", "Fiori")
        );
    }
}
