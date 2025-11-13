package com.application.canopy.model;

import java.util.List;

public class Plant {

    private final String id;           // univoco, es: "lavanda"
    private final String name;         // nome comune: "Lavanda"
    private final String latinName;    // es: "Lavandula angustifolia"
    private final String description;  // descrizione in erbario
    private final String careTips;     // "come prendersene cura"
    private final String folderName;   // cartella immagini: "Lavanda"
    private final String thumbFile;    // thumb: "Lavanda.png"

    public Plant(String id,
                 String name,
                 String latinName,
                 String description,
                 String careTips,
                 String folderName,
                 String thumbFile) {
        this.id = id;
        this.name = name;
        this.latinName = latinName;
        this.description = description;
        this.careTips = careTips;
        this.folderName = folderName;
        this.thumbFile = thumbFile;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLatinName() { return latinName; }
    public String getDescription() { return description; }
    public String getCareTips() { return careTips; }
    public String getFolderName() { return folderName; }
    public String getThumbFile() { return thumbFile; }

    // Catalogo "statico" delle piante sbloccabili
    public static List<Plant> samplePlants() {
        return List.of(
                new Plant(
                                "sakura",
                                "Sakura",
                                "Prunus serrulata",
                                "",
                                "",
                                "Sakura",
                                "Sakura.png"
                        ),
                new Plant(
                                "quercia",
                                "Quercia",
                                "Quercus robur",
                                "Una pianta robusta che rappresenta stabilità e forza.",
                                "",
                                "Quercia",
                                "Quercia.png"
                        ),
                new Plant(
                                "menta",
                                "Menta",
                                "Mentha spicata",
                                "",
                                "",
                                "Menta",
                                "Menta.png"
                        ),
                new Plant(
                                "lavanda",
                                "Lavanda",
                                "Lavandula angustifolia",
                                "",
                                "",
                                "Lavanda",
                                "Lavanda.png"
                        ),
                new Plant(
                                "peperoncino",
                                "Peperoncino",
                                "Capsicum annuum",
                                "",
                                "",
                                "Peperoncino",
                                "Peperoncino.png"
                        ),
                new Plant(
                                "orchidea",
                                "Orchidea",
                                "Orchidaceae",
                                "",
                                "",
                                "Orchidea",
                                "Orchidea.png"
                        ),
                new Plant(
                                "ceneradice",
                                "Ceneradice",
                                "Cineris radix",
                                "",
                                "",
                                "Ceneradice",
                                "Ceneradice.png"
                        ),
                new Plant(
                                "radice_sussurrante",
                                "Radice Sussurrante",
                                "Radix susurrans",
                                "",
                                "",
                                "Radice_Sussurrante",
                                "Radice_Sussurrante.png"
                        )
        );
    }
}
