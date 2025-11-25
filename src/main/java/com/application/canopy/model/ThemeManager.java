package com.application.canopy.model;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class ThemeManager {

    public enum Theme {
        DARK, LIGHT
    }

    private static final String PREF_KEY = "canopy.theme";

    private static Theme currentTheme = Theme.DARK;

    // ----- LISTENER CAMBIO TEMA -----
    private static final List<Consumer<Theme>> listeners = new ArrayList<>();

    public static void addThemeListener(Consumer<Theme> listener) {
        if (listener == null) return;
        listeners.add(listener);
        // gli mando SUBITO il tema corrente, così si inizializza correttamente
        listener.accept(currentTheme);
    }

    private static void notifyThemeListeners() {
        for (Consumer<Theme> l : listeners) {
            l.accept(currentTheme);
        }
    }

    // Blocco statico: eseguito una volta sola quando la classe viene caricata
    static {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            String saved = prefs.get(PREF_KEY, Theme.DARK.name()); // default DARK
            currentTheme = Theme.valueOf(saved);
        } catch (Exception e) {
            currentTheme = Theme.DARK; // fallback sicuro
        }
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    /** Salva il tema corrente nelle preferenze utente */
    private static void saveCurrentTheme() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_KEY, currentTheme.name());
        } catch (Exception e) {
            // se qualcosa va storto, pazienza: semplicemente non ricordiamo il tema
        }
    }

    /** Applica la classe CSS corretta al root passato */
    public static void applyTheme(Parent root) {
        if (root == null) return;

        var styles = root.getStyleClass();
        styles.removeAll("theme-dark", "theme-light");

        if (currentTheme == Theme.DARK) {
            styles.add("theme-dark");
        } else {
            styles.add("theme-light");
        }
    }

    /** Toggle globale per la scena corrente (usato eventualmente dalla nav se tenessi un bottone) */
    public static void toggle(Scene scene) {
        if (scene == null) return;

        currentTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        saveCurrentTheme();
        applyTheme(scene.getRoot());
        notifyThemeListeners();
    }

    /** Imposta esplicitamente il tema (usato da Settings) */
    public static void setTheme(Theme theme, Scene scene) {
        if (theme == null) return;

        currentTheme = theme;
        saveCurrentTheme();

        if (scene != null) {
            applyTheme(scene.getRoot());
        }

        notifyThemeListeners();
    }
}
