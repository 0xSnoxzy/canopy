package com.application.canopy.model;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class ThemeManager {

    // =======================
    //  PREFERENZE
    // =======================

    private static final String PREF_THEME_KEY = "canopy.themeId";
    private static final String PREF_CVD_KEY   = "canopy.cvdFilter";

    /**
     * themeId supportati:
     *  - "dark", "light"                       -> Evergreen scuro/chiaro
     *  - "sakura-light", "sakura-dark"
     *  - "quercia-light", "quercia-dark"
     *  - "menta-light", "menta-dark"
     *  - "peperoncino-light", "peperoncino-dark"
     *  - "lavanda-light", "lavanda-dark"
     *  - "orchidea-light", "orchidea-dark"
     *
     * NB: il vecchio tema "daltonici-*" è stato rimosso: ora usiamo filtri CVD.
     */
    private static final Set<String> SUPPORTED_THEMES = Set.of(
            "dark",
            "light",
            "sakura-light", "sakura-dark",
            "quercia-light", "quercia-dark",
            "menta-light", "menta-dark",
            "peperoncino-light", "peperoncino-dark",
            "lavanda-light", "lavanda-dark",
            "orchidea-light", "orchidea-dark"
    );

    /** Filtri daltonismo supportati */
    private static final Set<String> SUPPORTED_CVD = Set.of(
            "none",
            "deuteranopia",
            "protanopia",
            "tritanopia"
    );

    // di default: evergreen dark, nessun filtro CVD
    private static String currentThemeId    = "dark";
    private static String currentCvdFilter  = "none";

    // listener che ascoltano i cambi di tema (Nav, ecc.)
    private static final List<Consumer<String>> listeners = new ArrayList<>();

    // =======================
    //  STATIC INIT
    // =======================

    static {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

            String savedTheme = prefs.get(PREF_THEME_KEY, "dark");
            savedTheme = savedTheme.toLowerCase(Locale.ROOT);
            if (SUPPORTED_THEMES.contains(savedTheme)) {
                currentThemeId = savedTheme;
            } else {
                currentThemeId = "dark";
            }

            String savedCvd = prefs.get(PREF_CVD_KEY, "none");
            savedCvd = savedCvd.toLowerCase(Locale.ROOT);
            if (SUPPORTED_CVD.contains(savedCvd)) {
                currentCvdFilter = savedCvd;
            } else {
                currentCvdFilter = "none";
            }

        } catch (Exception e) {
            currentThemeId = "dark";
            currentCvdFilter = "none";
        }
    }

    // =======================
    //  API PUBBLICA TEMA
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

    /** Ritorna la "palette": evergreen / sakura / quercia / menta / peperoncino / lavanda / orchidea */
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

    /** Applica la classe CSS corretta al root (tema + eventuale filtro CVD) */
    public static void applyTheme(Parent root) {
        if (root == null) return;

        var styles = root.getStyleClass();
        styles.removeAll(
                // temi
                "theme-dark",
                "theme-light",
                "theme-sakura-light", "theme-sakura-dark",
                "theme-quercia-light", "theme-quercia-dark",
                "theme-menta-light", "theme-menta-dark",
                "theme-peperoncino-light", "theme-peperoncino-dark",
                "theme-lavanda-light", "theme-lavanda-dark",
                "theme-orchidea-light", "theme-orchidea-dark",
                // filtri CVD
                "cvd-deuteranopia",
                "cvd-protanopia",
                "cvd-tritanopia"
        );

        // classe tema botanico / modalità
        String cssClass = "theme-" + currentThemeId;
        if (!styles.contains(cssClass)) {
            styles.add(cssClass);
        }

        // eventuale filtro daltonismo
        if (!"none".equals(currentCvdFilter)) {
            String cvdClass = "cvd-" + currentCvdFilter;
            if (!styles.contains(cvdClass)) {
                styles.add(cvdClass);
            }
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
                "orchidea"
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
    //  API PUBBLICA – FILTRI DALTONISMO
    // =======================

    /** Ritorna il filtro CVD corrente: "none", "deuteranopia", "protanopia", "tritanopia" */
    public static String getCurrentColorVisionFilter() {
        return currentCvdFilter;
    }

    /** Imposta il filtro daltonismo e ri-applica il tema alla scena */
    public static void setColorVisionFilter(String filterId, Scene scene) {
        if (filterId == null) filterId = "none";
        filterId = filterId.toLowerCase(Locale.ROOT);

        if (!SUPPORTED_CVD.contains(filterId)) {
            filterId = "none";
        }

        currentCvdFilter = filterId;
        saveCurrentCvdFilter();

        if (scene != null) {
            applyTheme(scene.getRoot());
        }

        // i listener ricevono sempre il themeId; il filtro è "in più"
        notifyThemeListeners();
    }

    // =======================
    //  PRIVATI
    // =======================

    private static void saveCurrentTheme() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_THEME_KEY, currentThemeId);
        } catch (Exception ignored) {}
    }

    private static void saveCurrentCvdFilter() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_CVD_KEY, currentCvdFilter);
        } catch (Exception ignored) {}
    }

    private static void notifyThemeListeners() {
        for (Consumer<String> l : listeners) {
            l.accept(currentThemeId);
        }
    }
}
