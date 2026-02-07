package com.application.canopy.util;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestisce il caricamento delle risorse.
 */
public class ResourceManager {

    private static final String IMAGES_ROOT = "/com/application/canopy/view/components/images/";
    private static final String THUMBS_DIR = IMAGES_ROOT + "thumbs/";
    // private static final String PLANTS_DIR = IMAGES_ROOT + "plants/";

    private static final Map<String, Image> imageCache = new HashMap<>();

    public static Image loadImage(String path) {
        if (path == null)
            return null;
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }

        URL url = ResourceManager.class.getResource(path);
        if (url == null) {
            System.err.println("[ResourceManager] Risorsa non trovata: " + path);
            return null;
        }

        Image img = new Image(url.toExternalForm(), false);
        imageCache.put(path, img);
        return img;
    }

    /**
     * Tenta di caricare un'immagine da più percorsi possibili.
     * Restituisce la prima trovata.
     */
    public static Image loadFirstExisting(String... paths) {
        for (String path : paths) {
            URL url = ResourceManager.class.getResource(path);
            if (url != null) {
                return loadImage(path); // usa la cache
            }
        }
        return null;
    }

    public static Image getPlantThumbVariant(String baseName, String suffix) {
        // Cerca sia png che jpg
        return loadFirstExisting(
                THUMBS_DIR + baseName + suffix + ".png",
                THUMBS_DIR + baseName + suffix + ".jpg");
    }

    public static Image getPlantThumbnail(String fileName) {
        return loadImage(THUMBS_DIR + fileName);
    }

    public static Image getPlantThumbnailByName(String plantName) {
        if (plantName == null)
            return null;

        // Mappatura nome -> file
        String key = plantName.toLowerCase().trim();
        String fName = switch (key) {
            case "lavanda" -> "Lavanda.png";
            case "menta" -> "Menta.png";
            case "orchidea" -> "Orchidea.png";
            case "peperoncino" -> "Peperoncino.png";
            case "quercia" -> "Quercia.png";
            case "sakura" -> "Sakura.png";
            case "lifeblood" -> "Lifeblood.png";
            case "radice_sussurrante" -> "Radici_sussurranti.png";
            default -> null;
        };

        if (fName == null) {
            fName = Character.toUpperCase(key.charAt(0)) + key.substring(1) + ".png";
        }

        return getPlantThumbnail(fName);
    }

    public static Image getGrowthImage(com.application.canopy.model.Plant plant, int stageIndex) {
        if (plant == null)
            return null;

        // Stage 3 -> Pianta finale (usa la thumb normale)
        if (stageIndex >= 3) {
            return getPlantThumbnail(plant.getThumbFile());
        }

        java.util.Set<String> candidates = new java.util.LinkedHashSet<>();

        if (plant.getFolderName() != null && !plant.getFolderName().isEmpty()) {
            candidates.add(plant.getFolderName());
        }
        candidates.add(plant.getName());
        candidates.add(plant.getName().replace(" ", "_"));

        String thumb = plant.getThumbFile();
        if (thumb != null && thumb.contains(".")) {
            String fromThumb = thumb.substring(0, thumb.lastIndexOf('.'));
            candidates.add(fromThumb);
        }

        String suffix = "-Stage" + stageIndex;

        for (String base : candidates) {
            Image img = loadFirstExisting(
                    THUMBS_DIR + base + suffix + ".png",
                    THUMBS_DIR + base + suffix + ".jpg");
            if (img != null) {
                return img;
            }
        }

        System.err.println(
                "[ResourceManager] Immagine stage non trovata: " + plant.getName() + " stage: " + stageIndex);
        return null;
    }

    public static Image getNavIcon(String name, boolean isWhite) {
        String path = IMAGES_ROOT + name + (isWhite ? "-dark" : "") + ".png";
        return loadImage(path);
    }
}
