package com.application.canopy.controller;

import com.application.canopy.db.DatabaseManager;
import com.application.canopy.db.PlantActivityRepository;
import com.application.canopy.model.FontManager;
import com.application.canopy.model.FontManager.AppFont;
import com.application.canopy.model.GameState;        // 👈 IMPORT NUOVO
import com.application.canopy.model.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.sql.SQLException;

public class SettingsController {

    @FXML
    private BorderPane root;

    @FXML
    private ComboBox<String> fontCombo;

    @FXML
    private ComboBox<String> modeCombo;   // modalità light/dark

    @FXML
    private ComboBox<String> themeCombo;  // tema botanico

    @FXML
    private ComboBox<String> daltonismoCombo; // filtro daltonismo (CVD)

    @FXML
    private void initialize() {
        setupFontCombo();
        setupModeCombo();
        setupThemeCombo();
        setupDaltonismoCombo();
    }

    // ============================
    // RESET DATI: CALENDARIO + GAMESTATE (OBIETTIVI, PROGRESSI, ECC.)
    // ============================
    @FXML
    private void onResetCalendarStats() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset dati di gioco");
        confirm.setHeaderText("Vuoi davvero cancellare TUTTE le statistiche?");
        confirm.setContentText("""
                Verranno resettati:
                • Calendario (tutti i minuti per pianta)
                • Progressi di gioco (pomodori, best plant, ecc.)
                • Di conseguenza anche gli obiettivi torneranno a non completati

                Questa operazione non può essere annullata.
                """);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // 1) Svuota la tabella del calendario
            PlantActivityRepository repo =
                    new PlantActivityRepository(DatabaseManager.getConnection());
            repo.deleteAll();

            // 2) Resetta anche lo stato di gioco (pomodori, best-of-day, ecc.)
            GameState.getInstance().resetAllProgress();

        } catch (SQLException e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Errore");
            error.setHeaderText("Impossibile resettare i dati di gioco.");
            error.setContentText("Dettagli: " + e.getMessage());
            error.showAndWait();
            return;
        }

        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setTitle("Dati resettati");
        ok.setHeaderText(null);
        ok.setContentText("Calendario e progressi di gioco sono stati resettati.");
        ok.showAndWait();
    }

    // ============================
    // FONT
    // ============================
    private void setupFontCombo() {
        if (fontCombo == null) return;

        fontCombo.getItems().setAll(
                AppFont.ATKINSON.getDisplayName(),
                AppFont.COMIC_NEUE.getDisplayName(),
                AppFont.ROBOTO_MONO.getDisplayName(),
                AppFont.NOTO_SERIF.getDisplayName()
        );

        AppFont current = FontManager.getCurrentFont();
        fontCombo.getSelectionModel().select(current.getDisplayName());

        fontCombo.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal == null) return;
                    AppFont selected = AppFont.fromDisplayName(newVal);

                    if (root != null && root.getScene() != null) {
                        FontManager.setFont(selected, root.getScene());
                    }
                });
    }

    // ============================
    // MODALITÀ (LIGHT / DARK)
    // ============================
    private void setupModeCombo() {
        if (modeCombo == null) return;

        modeCombo.getItems().setAll("Chiara", "Scura");

        String currentMode = ThemeManager.getCurrentMode(); // "light" o "dark"
        modeCombo.getSelectionModel().select(
                "dark".equals(currentMode) ? "Scura" : "Chiara"
        );

        modeCombo.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal == null) return;
                    if (root == null) return;

                    Scene scene = root.getScene();
                    if (scene == null) return;

                    String modeId = "Scura".equals(newVal) ? "dark" : "light";
                    ThemeManager.setMode(modeId, scene);
                });
    }

    // ============================
    // TEMA BOTANICO
    // ============================
    private void setupThemeCombo() {
        if (themeCombo == null) return;

        themeCombo.getItems().setAll(
                "Evergreen",
                "Sakura",
                "Quercia",
                "Menta",
                "Peperoncino",
                "Lavanda",
                "Orchidea"
        );

        String currentPalette = ThemeManager.getCurrentPalette(); // evergreen / sakura / ...
        String label = switch (currentPalette) {
            case "sakura"      -> "Sakura";
            case "quercia"     -> "Quercia";
            case "menta"       -> "Menta";
            case "peperoncino" -> "Peperoncino";
            case "lavanda"     -> "Lavanda";
            case "orchidea"    -> "Orchidea";
            default            -> "Evergreen";
        };
        themeCombo.getSelectionModel().select(label);

        themeCombo.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal == null) return;
                    if (root == null) return;

                    Scene scene = root.getScene();
                    if (scene == null) return;

                    String paletteId = switch (newVal) {
                        case "Sakura"      -> "sakura";
                        case "Quercia"     -> "quercia";
                        case "Menta"       -> "menta";
                        case "Peperoncino" -> "peperoncino";
                        case "Lavanda"     -> "lavanda";
                        case "Orchidea"    -> "orchidea";
                        default            -> "evergreen";
                    };

                    ThemeManager.setPalette(paletteId, scene);
                });
    }

    // ============================
    // FILTRO DALTONISMO
    // ============================
    private void setupDaltonismoCombo() {
        if (daltonismoCombo == null) return;

        daltonismoCombo.getItems().setAll(
                "Nessun filtro",
                "Deuteranopia",
                "Protanopia",
                "Tritanopia"
        );

        // allinea alla preferenza salvata
        String current = ThemeManager.getCurrentColorVisionFilter(); // "none", "deuteranopia", ...
        String label = switch (current) {
            case "deuteranopia" -> "Deuteranopia";
            case "protanopia"   -> "Protanopia";
            case "tritanopia"   -> "Tritanopia";
            default             -> "Nessun filtro";
        };
        daltonismoCombo.getSelectionModel().select(label);

        daltonismoCombo.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal == null) return;
                    if (root == null) return;

                    Scene scene = root.getScene();
                    if (scene == null) return;

                    String filterId = switch (newVal) {
                        case "Deuteranopia" -> "deuteranopia";
                        case "Protanopia"   -> "protanopia";
                        case "Tritanopia"   -> "tritanopia";
                        default             -> "none";
                    };

                    ThemeManager.setColorVisionFilter(filterId, scene);
                });
    }
}
