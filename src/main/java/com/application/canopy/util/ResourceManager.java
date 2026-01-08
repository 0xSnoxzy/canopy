package com.application.canopy.util;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestisce il caricamento delle risorse (immagini) in modo centralizzato.
 * Evita duplicazioni di percorso e try-catch ovunque.
 */
public class ResourceManager {

    private static final String IMAGES_ROOT = "/com/application/canopy/view/components/images/";
    private static final String THUMBS_DIR = IMAGES_ROOT + "thumbs/";
    private static final String PLANTS_DIR = IMAGES_ROOT + "plants/";

    // Cache semplice per evitare di ricaricare stesse immagini
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

        Image img = new Image(url.toExternalForm(), false); // background loading = false (evita glitch layout)
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
        // Mappatura semplice nome -> file (estendibile)
        // In un mondo ideale questo sta nel DB o nel model Plant,
        // ma per ora supportiamo la logica legacy dei controller.
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
            // Tentativo generico
            fName = Character.toUpperCase(key.charAt(0)) + key.substring(1) + ".png";
        }

        return getPlantThumbnail(fName);
    }

    public static Image getStageImage(String folderName, int stageIndex) {
        String filename = "stage" + stageIndex + ".png";
        return loadImage(PLANTS_DIR + folderName + "/" + filename);
    }

    /**
     * Carica l'icona della navigazione.
     * 
     * @param name    es. "home", "settings"
     * @param isWhite se true carica la versione bianca (es. "home-dark.png"),
     *                altrimenti nera ("home.png")
     *                NB: La nomenclatura dei file è un po' controintuitiva:
     *                "dark.png" è bianca (per sfondo scuro).
     */
    public static Image getNavIcon(String name, boolean isWhite) {
        String path = IMAGES_ROOT + name + (isWhite ? "-dark" : "") + ".png";
        return loadImage(path);
    }
}
