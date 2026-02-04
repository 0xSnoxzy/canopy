package com.application.canopy.model;

import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.text.Font;

import java.util.Objects;
import java.util.prefs.Preferences;

public final class FontManager {

    private FontManager() {
    }

    // definisce i font disponibili nell'applicazione
    public enum AppFont {
        ATKINSON("Atkinson Hyperlegible", "/css/fonts/AtkinsonHyperlegible-Regular.ttf", "font-atkinson"),
        COMIC_NEUE("Comic Neue", "/css/fonts/ComicNeue-Regular.ttf", "font-comicneue"),
        ROBOTO_MONO("Roboto Mono", "/css/fonts/RobotoMono-Regular.ttf", "font-robotomono"),
        NOTO_SERIF("Noto Serif", "/css/fonts/NotoSerif-Regular.ttf", "font-notoserif");

        private final String displayName;
        private final String resourcePath;
        private final String cssClass;

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

        public static AppFont fromDisplayName(String name) {
            for (AppFont f : values()) {
                if (f.displayName.equals(name))
                    return f;
            }
            return ATKINSON;
        }
    }

    private static final String PREF_KEY = "canopy.font";
    private static final Preferences PREFS = Preferences.userNodeForPackage(FontManager.class);

    private static AppFont currentFont;

    // inizializzazione
    public static void initFonts() {
        // initFonts prova a caricare i file .ttf dalla cartella risorse.
        // questo è necessario perché javafx non vede i font custom se non vengono
        // caricati prima manualmente
        for (AppFont f : AppFont.values()) {
            Font font = Font.loadFont(
                    Objects.requireNonNull(
                            FontManager.class.getResourceAsStream(f.getResourcePath()),
                            "Font non trovato: " + f.getResourcePath()),
                    14);
            if (font != null) {
                System.out.println("Caricato font: " + f.getDisplayName() + " -> " + font.getName());
            } else {
                System.err.println("Impossibile caricare font: " + f.getResourcePath());
            }
        }

        // recupera l'ultimo font scelto dall'utente (o usa quello di default)
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

    // applicazione alla ui

    // applica logicamente il font alla scena (cambiando la classe css del root)
    public static void applyCurrentFont(Scene scene) {
        if (scene == null)
            return;
        Parent root = scene.getRoot();
        if (root == null)
            return;

        // rimuove eventuali classi vecchie relative ai font
        // questo serve a evitare che due font si sovrappongano o che il css vada in
        // confusione
        root.getStyleClass().removeAll(
                "font-atkinson",
                "font-comicneue",
                "font-robotomono",
                "font-notoserif");

        // aggiunge la classe del font corrente
        // la classe css (es. .font-comicneue) contiene la regola -fx-font-family che
        // cambia il font visibile
        AppFont font = getCurrentFont();
        String cssClass = font.getCssClass();
        if (!root.getStyleClass().contains(cssClass)) {
            root.getStyleClass().add(cssClass);
        }

        System.out.println("Applicato font alla scena via CSS class: " + font.getDisplayName());
    }

    // cambia il font corrente, salva la preferenza e aggiorna subito la scena
    public static void setFont(AppFont font, Scene scene) {
        if (font == null)
            return;
        currentFont = font;
        PREFS.put(PREF_KEY, font.name());

        if (scene != null) {
            applyCurrentFont(scene);
        }
    }
}
