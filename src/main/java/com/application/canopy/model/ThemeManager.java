package com.application.canopy.model;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class ThemeManager {

    // preferences

    private static final String PREF_THEME_KEY = "canopy.themeId";
    private static final String PREF_CVD_KEY = "canopy.cvdFilter";

    private static final Set<String> SUPPORTED_THEMES = Set.of(
            "dark",
            "light",
            "sakura-light", "sakura-dark",
            "quercia-light", "quercia-dark",
            "menta-light", "menta-dark",
            "peperoncino-light", "peperoncino-dark",
            "lavanda-light", "lavanda-dark",
            "orchidea-light", "orchidea-dark");

    // filtri daltonismo supportati
    private static final Set<String> SUPPORTED_CVD = Set.of(
            "none",
            "deuteranopia",
            "protanopia",
            "tritanopia");

    // tema di default: evergreen dark, nessun filtro daltonismo
    private static String currentThemeId = "dark";
    private static String currentCvdFilter = "none";

    // listener che ascoltano i cambi di tema
    private static final List<Consumer<String>> listeners = new ArrayList<>();

    static {
        try {
            // Preferences è una piccola memoria che serve a salvare le impostazioni
            // dell'utente (come il tema scelto) in modo che rimangano salvate
            // anche quando chiudi e riapri l'applicazione.
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

    // ritorna solo la modalità light o dark
    public static String getCurrentMode() {
        if ("dark".equals(currentThemeId) || currentThemeId.endsWith("-dark")) {
            return "dark";
        }
        return "light";
    }

    // ritorna la palette tema
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

    // Gestione Listeners

    // Aggiunge un listener che viene notificato quando il tema cambia
    public static void addThemeListener(Consumer<String> listener) {
        if (listener == null)
            return;
        listeners.add(listener);
        // Notifica subito il listener col tema attuale per allinearlo
        listener.accept(currentThemeId);
    }

    // Applicazione alla UI

    // Applica le classi CSS corrette alla root della scena
    // permette al file CSS di cambiare i colori di TUTTI i componenti dentro la
    // finestra
    public static void applyTheme(Parent root) {
        if (root == null)
            return;

        var styles = root.getStyleClass();
        // Rimuove tutte le possibili classi di tema precedenti per evitare conflitti
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
                // filtri daltonismo
                "cvd-deuteranopia",
                "cvd-protanopia",
                "cvd-tritanopia");

        // Aggiunge la classe del tema corrente per applicare i colori corrispondenti al tema
        String cssClass = "theme-" + currentThemeId;
        if (!styles.contains(cssClass)) {
            styles.add(cssClass);
        }

        // Se c'è un filtro daltonismo attivo, aggiunge anche la sua classe
        if (!"none".equals(currentCvdFilter)) {
            String cvdClass = "cvd-" + currentCvdFilter;
            if (!styles.contains(cvdClass)) {
                styles.add(cvdClass);
            }
        }
    }

    // setters (cambio tema)

    // imposta un tema specifico e aggiorna tutto
    public static void setTheme(String themeId, Scene scene) {
        if (themeId == null)
            return;

        themeId = themeId.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_THEMES.contains(themeId)) {
            themeId = "dark"; // fallback se il tema non esiste
        }

        currentThemeId = themeId;
        saveCurrentTheme(); // salva nelle preferenze utente

        if (scene != null) {
            applyTheme(scene.getRoot());
        }

        notifyThemeListeners();
    }

    // cambia solo la modalità (light/dark) mantenendo la palette corrente
    public static void setMode(String mode, Scene scene) {
        if (mode == null)
            return;
        mode = mode.toLowerCase(Locale.ROOT);
        if (!mode.equals("light") && !mode.equals("dark"))
            return;

        String palette = getCurrentPalette();
        String newThemeId;

        if ("evergreen".equals(palette)) {
            // il tema base ha solo "dark" e "light" senza prefisso
            newThemeId = mode;
        } else {
            // gli altri temi sono composti da "palette-modalità"
            newThemeId = palette + "-" + mode;
        }

        setTheme(newThemeId, scene);
    }

    // cambia solo la palette mantenendo la modalità corrente
    public static void setPalette(String palette, Scene scene) {
        if (palette == null)
            return;
        palette = palette.toLowerCase(Locale.ROOT);

        // controlla se è una palette valida
        if (!Set.of(
                "evergreen",
                "sakura",
                "quercia",
                "menta",
                "peperoncino",
                "lavanda",
                "orchidea").contains(palette)) {
            palette = "evergreen";
        }

        String mode = getCurrentMode();
        String newThemeId;

        if ("evergreen".equals(palette)) {
            newThemeId = mode;
        } else {
            newThemeId = palette + "-" + mode;
        }

        setTheme(newThemeId, scene);
    }

    // filtri daltonismo

    public static String getCurrentColorVisionFilter() {
        return currentCvdFilter;
    }

    // attiva un filtro per daltonismo
    public static void setColorVisionFilter(String filterId, Scene scene) {
        if (filterId == null)
            filterId = "none";
        filterId = filterId.toLowerCase(Locale.ROOT);

        if (!SUPPORTED_CVD.contains(filterId)) {
            filterId = "none";
        }

        currentCvdFilter = filterId;
        saveCurrentCvdFilter();

        if (scene != null) {
            applyTheme(scene.getRoot());
        }

        notifyThemeListeners();
    }

    // metodi privati di supporto

    // salva il tema scelto nella memoria permanente
    private static void saveCurrentTheme() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_THEME_KEY, currentThemeId);
        } catch (Exception ignored) {
        }
    }

    // salva il filtro daltonismo scelto nella memoria permanente
    private static void saveCurrentCvdFilter() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_CVD_KEY, currentCvdFilter);
        } catch (Exception ignored) {
        }
    }

    // avvisa tutte le parti dell'app che il tema è cambiato
    private static void notifyThemeListeners() {
        for (Consumer<String> l : listeners) {
            l.accept(currentThemeId);
        }
    }
}
