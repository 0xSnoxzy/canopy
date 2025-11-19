package com.application.canopy.model;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class ThemeManager {

    public enum Theme {
        DARK, LIGHT
    }

    private static Theme currentTheme = Theme.DARK;

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    /** Applica la classe CSS corretta al root passato */
    public static void applyTheme(Parent root) {
        var styles = root.getStyleClass();
        styles.removeAll("theme-dark", "theme-light");

        if (currentTheme == Theme.DARK) {
            styles.add("theme-dark");
        } else {
            styles.add("theme-light");
        }
    }

    /** Toggle globale per la scena corrente */
    public static void toggle(Scene scene) {
        if (scene == null) return;

        // cambia stato globale
        currentTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;

        // ri-applica al root della scena
        applyTheme(scene.getRoot());
    }
}
