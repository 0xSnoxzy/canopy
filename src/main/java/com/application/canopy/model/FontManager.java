package com.application.canopy.model;

import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.text.Font;

import java.util.Objects;
import java.util.prefs.Preferences;

public final class FontManager {

    private FontManager() {}

    public enum AppFont {
        ATKINSON("Atkinson Hyperlegible", "/css/fonts/AtkinsonHyperlegible-Regular.ttf", "font-atkinson"),
        COMIC_NEUE("Comic Neue", "/css/fonts/ComicNeue-Regular.ttf", "font-comicneue"),
        ROBOTO_MONO("Roboto Mono", "/css/fonts/RobotoMono-Regular.ttf", "font-robotomono"),
        NOTO_SERIF("Noto Serif", "/css/fonts/NotoSerif-Regular.ttf", "font-notoserif");

        private final String displayName;
        private final String resourcePath;
        private final String cssClass;   // classe CSS da mettere sulla root
        private String fxName;           // nome effettivo del font in JavaFX

        AppFont(String displayName, String resourcePath, String cssClass) {
            this.displayName = displayName;
            this.resourcePath = resourcePath;
            this.cssClass = cssClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getResourcePath() {
            return resourcePath;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getFxName() {
            return fxName != null ? fxName : displayName;
        }

        void setFxName(String fxName) {
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
    //  APPLICA FONT ALLA SCENA (via classi CSS)
    // ========================================================
    public static void applyCurrentFont(Scene scene) {
        if (scene == null) return;
        Parent root = scene.getRoot();
        if (root == null) return;

        // togliamo tutte le classi font-*
        root.getStyleClass().removeAll(
                "font-atkinson",
                "font-comicneue",
                "font-robotomono",
                "font-notoserif"
        );

        // aggiungiamo quella corretta
        AppFont font = getCurrentFont();
        String cssClass = font.getCssClass();
        if (!root.getStyleClass().contains(cssClass)) {
            root.getStyleClass().add(cssClass);
        }

        System.out.println("Applicato font alla scena via CSS class: " + font.getDisplayName());
    }

    public static void setFont(AppFont font, Scene scene) {
        if (font == null) return;
        currentFont = font;
        PREFS.put(PREF_KEY, font.name());

        if (scene != null) {
            applyCurrentFont(scene);
        }
    }
}
