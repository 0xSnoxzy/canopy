package com.application.canopy.model;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static final String PREF_KEY = "canopy.themeId";

    /**
     * themeId supportati:
     *  - "dark", "light"                       -> Evergreen scuro/chiaro
     *  - "sakura-light", "sakura-dark"
     *  - "quercia-light", "quercia-dark"
     *  - "menta-light", "menta-dark"
     *  - "peperoncino-light", "peperoncino-dark"
     *  - "lavanda-light", "lavanda-dark"
     *  - "orchidea-light", "orchidea-dark"
     *  - "daltonici-light", "daltonici-dark"
     */
    private static final Set<String> SUPPORTED_THEMES = Set.of(
            "dark",
            "light",
            "sakura-light", "sakura-dark",
            "quercia-light", "quercia-dark",
            "menta-light", "menta-dark",
            "peperoncino-light", "peperoncino-dark",
            "lavanda-light", "lavanda-dark",
            "orchidea-light", "orchidea-dark",
            "daltonici-light", "daltonici-dark"
    );

    // di default: evergreen dark
    private static String currentThemeId = "dark";

    // listener che ascoltano i cambi di tema (Nav, ecc.)
    private static final List<Consumer<String>> listeners = new ArrayList<>();

    static {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            String saved = prefs.get(PREF_KEY, "dark");
            saved = saved.toLowerCase(Locale.ROOT);
            if (SUPPORTED_THEMES.contains(saved)) {
                currentThemeId = saved;
            } else {
                currentThemeId = "dark";
            }
        } catch (Exception e) {
            currentThemeId = "dark";
        }
    }

    // =======================
    //  API PUBBLICA
    // =======================

    public static String getCurrentThemeId() {
        return currentThemeId;
    }

    /** Ritorna solo la modalità: "light" o "dark" */
    public static String getCurrentMode() {
        if ("dark".equals(currentThemeId) || currentThemeId.endsWith("-dark")) {
            return "dark";
        }
        return "light";
    }

    /** Ritorna la "palette": evergreen / sakura / quercia / ... */
    public static String getCurrentPalette() {
        return switch (currentThemeId) {
            case "dark", "light" -> "evergreen";
            default -> {
                int dash = currentThemeId.indexOf('-');
                if (dash > 0) {
                    yield currentThemeId.substring(0, dash);
                } else {
                    yield "evergreen";
                }
            }
        };
    }

    /** Listener per i cambi di tema. Riceve sempre il themeId (es. "sakura-dark"). */
    public static void addThemeListener(Consumer<String> listener) {
        if (listener == null) return;
        listeners.add(listener);
        // allinea subito col tema corrente
        listener.accept(currentThemeId);
    }

    /** Applica la classe CSS corretta al root */
    public static void applyTheme(Parent root) {
        if (root == null) return;

        var styles = root.getStyleClass();
        styles.removeAll(
                "theme-dark",
                "theme-light",
                "theme-sakura-light", "theme-sakura-dark",
                "theme-quercia-light", "theme-quercia-dark",
                "theme-menta-light", "theme-menta-dark",
                "theme-peperoncino-light", "theme-peperoncino-dark",
                "theme-lavanda-light", "theme-lavanda-dark",
                "theme-orchidea-light", "theme-orchidea-dark",
                "theme-daltonici-light", "theme-daltonici-dark"
        );

        String cssClass = "theme-" + currentThemeId;
        if (!styles.contains(cssClass)) {
            styles.add(cssClass);
        }
    }

    /** Imposta direttamente un themeId supportato */
    public static void setTheme(String themeId, Scene scene) {
        if (themeId == null) return;

        themeId = themeId.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_THEMES.contains(themeId)) {
            themeId = "dark";
        }

        currentThemeId = themeId;
        saveCurrentTheme();

        if (scene != null) {
            applyTheme(scene.getRoot());
        }

        notifyThemeListeners();
    }

    /** Helper: cambia solo la modalità (light/dark) mantenendo la palette */
    public static void setMode(String mode, Scene scene) {
        if (mode == null) return;
        mode = mode.toLowerCase(Locale.ROOT);
        if (!mode.equals("light") && !mode.equals("dark")) return;

        String palette = getCurrentPalette();
        String newThemeId;

        if ("evergreen".equals(palette)) {
            // evergreen rimane mappato su "dark" / "light"
            newThemeId = mode;
        } else {
            newThemeId = palette + "-" + mode;
        }

        setTheme(newThemeId, scene);
    }

    /** Helper: cambia solo la palette mantenendo la modalità */
    public static void setPalette(String palette, Scene scene) {
        if (palette == null) return;
        palette = palette.toLowerCase(Locale.ROOT);

        // palette supportate
        if (!Set.of(
                "evergreen",
                "sakura",
                "quercia",
                "menta",
                "peperoncino",
                "lavanda",
                "orchidea",
                "daltonici"
        ).contains(palette)) {
            palette = "evergreen";
        }

        String mode = getCurrentMode();
        String newThemeId;

        if ("evergreen".equals(palette)) {
            newThemeId = mode; // "dark"/"light"
        } else {
            newThemeId = palette + "-" + mode;
        }

        setTheme(newThemeId, scene);
    }

    // =======================
    //  PRIVATI
    // =======================

    private static void saveCurrentTheme() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_KEY, currentThemeId);
        } catch (Exception ignored) {}
    }

    private static void notifyThemeListeners() {
        for (Consumer<String> l : listeners) {
            l.accept(currentThemeId);
        }
    }
}
