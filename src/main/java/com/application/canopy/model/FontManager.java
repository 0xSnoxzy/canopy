package com.application.canopy.model;

import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.util.Objects;
import java.util.prefs.Preferences;

public final class FontManager {

    private FontManager() {}

    public enum AppFont {
        ATKINSON("Atkinson Hyperlegible", "/css/fonts/AtkinsonHyperlegible-Regular.ttf"),
        COMIC_NEUE("Comic Neue", "/css/fonts/ComicNeue-Regular.ttf"),
        ROBOTO_MONO("Roboto Mono", "/css/fonts/RobotoMono-Regular.ttf");

        private final String displayName;
        private final String resourcePath;
        private String fxName; // nome effettivo del font in JavaFX

        AppFont(String displayName, String resourcePath) {
            this.displayName = displayName;
            this.resourcePath = resourcePath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getResourcePath() {
            return resourcePath;
        }

        public String getFxName() {
            return fxName != null ? fxName : displayName;
        }

        private void setFxName(String fxName) {
            this.fxName = fxName;
        }

        public static AppFont fromDisplayName(String name) {
            for (AppFont f : values()) {
                if (f.displayName.equals(name)) return f;
            }
            return ATKINSON;
        }
    }

    private static final String PREF_KEY = "canopy.font";
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(FontManager.class);

    private static AppFont currentFont;

    // ========================================================
    //  INIT: da chiamare UNA VOLTA nel Main
    // ========================================================
    public static void initFonts() {
        // Carica i font e salva i loro nomi effettivi
        for (AppFont f : AppFont.values()) {
            Font font = Font.loadFont(
                    Objects.requireNonNull(
                            FontManager.class.getResourceAsStream(f.getResourcePath()),
                            "Font non trovato: " + f.getResourcePath()
                    ),
                    14
            );
            if (font != null) {
                f.setFxName(font.getName());
                System.out.println("Caricato font: " + f.getDisplayName() + " -> " + font.getName());
            } else {
                System.err.println("Impossibile caricare font: " + f.getResourcePath());
            }
        }

        // Carica la scelta dell'utente dalle Preferences
        String saved = PREFS.get(PREF_KEY, AppFont.ATKINSON.name());
        try {
            currentFont = AppFont.valueOf(saved);
        } catch (IllegalArgumentException e) {
            currentFont = AppFont.ATKINSON;
        }

        System.out.println("Font corrente da prefs: " + currentFont.name());
    }

    public static AppFont getCurrentFont() {
        if (currentFont == null) {
            currentFont = AppFont.ATKINSON;
        }
        return currentFont;
    }

    // ========================================================
    //  APPLICA FONT ALLA SCENA
    // ========================================================
    public static void applyCurrentFont(Scene scene) {
        if (scene == null) return;

        AppFont font = getCurrentFont();
        String fxName = font.getFxName();

        String style = scene.getRoot().getStyle();
        // sovrascriviamo solo il font-family, ignorando il resto
        // per semplicità, qui rimpiazziamo tutto lo style root, tanto di solito è vuoto
        scene.getRoot().setStyle("-fx-font-family: '" + fxName + "';");
        System.out.println("Applicato font alla scena: " + fxName);
    }

    public static void setFont(AppFont font, Scene scene) {
        if (font == null || scene == null) return;

        currentFont = font;
        PREFS.put(PREF_KEY, font.name());
        applyCurrentFont(scene);
    }
}
