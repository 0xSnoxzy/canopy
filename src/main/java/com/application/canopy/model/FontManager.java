package com.application.canopy.model;

import javafx.scene.Parent;

import java.util.prefs.Preferences;

public final class FontManager {

    private FontManager() {}

    public enum AppFont {
        ATKINSON("Atkinson Hyperlegible", "font-atkinson"),
        COMIC_NEUE("Comic Neue", "font-comicneue"),
        ROBOTO_MONO("Roboto Mono", "font-robotomono");

        private final String displayName;
        private final String cssClass;

        AppFont(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
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

    private static AppFont currentFont = loadFromPrefs();

    private static AppFont loadFromPrefs() {
        String saved = PREFS.get(PREF_KEY, AppFont.ATKINSON.name());
        try {
            return AppFont.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return AppFont.ATKINSON;
        }
    }

    public static AppFont getCurrentFont() {
        return currentFont;
    }

    public static void setFont(AppFont font, Parent root) {
        if (font == null) return;
        currentFont = font;
        PREFS.put(PREF_KEY, font.name());
        applyFont(root);
    }

    public static void applyFont(Parent root) {
        if (root == null) return;

        var styleClasses = root.getStyleClass();
        styleClasses.removeAll(
                AppFont.ATKINSON.getCssClass(),
                AppFont.COMIC_NEUE.getCssClass(),
                AppFont.ROBOTO_MONO.getCssClass()
        );
        styleClasses.add(currentFont.getCssClass());
    }
}
