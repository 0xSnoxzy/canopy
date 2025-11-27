package com.application.canopy.controller;

import com.application.canopy.db.DatabaseManager;
import com.application.canopy.db.PlantActivityRepository;
import com.application.canopy.model.FontManager;
import com.application.canopy.model.FontManager.AppFont;
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
    private ComboBox<String> modeCombo;   // nuova combo modalità

    @FXML
    private ComboBox<String> themeCombo;  // combo tema botanico

    @FXML
    private void initialize() {
        setupFontCombo();
        setupModeCombo();
        setupThemeCombo();
    }

    // ============================
    // RESET STATISTICHE CALENDARIO (DB)
    // ============================
    @FXML
    private void onResetCalendarStats() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Svuota statistiche calendario");
        confirm.setHeaderText("Vuoi davvero cancellare tutte le statistiche del calendario?");
        confirm.setContentText("Questa operazione non può essere annullata.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            PlantActivityRepository repo =
                    new PlantActivityRepository(DatabaseManager.getConnection());
            repo.deleteAll();
        } catch (SQLException e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Errore");
            error.setHeaderText("Impossibile svuotare le statistiche del calendario.");
            error.setContentText("Dettagli: " + e.getMessage());
            error.showAndWait();
            return;
        }

        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setTitle("Statistiche svuotate");
        ok.setHeaderText(null);
        ok.setContentText("Tutte le statistiche del calendario sono state cancellate.");
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
                AppFont.SPACE_MONO.getDisplayName(),
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
                "Orchidea",
                "Daltonici"
        );

        String currentPalette = ThemeManager.getCurrentPalette(); // evergreen / sakura / ...
        String label = switch (currentPalette) {
            case "sakura"      -> "Sakura";
            case "quercia"     -> "Quercia";
            case "menta"       -> "Menta";
            case "peperoncino" -> "Peperoncino";
            case "lavanda"     -> "Lavanda";
            case "orchidea"    -> "Orchidea";
            case "daltonici"   -> "Daltonici";
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
                        case "Daltonici"   -> "daltonici";
                        default            -> "evergreen";
                    };

                    ThemeManager.setPalette(paletteId, scene);
                });
    }
}
