package com.application.canopy.controller;

import com.application.canopy.model.FontManager;
import com.application.canopy.model.FontManager.AppFont;
import com.application.canopy.model.ThemeManager;
import com.application.canopy.model.ThemeManager.Theme;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;

public class SettingsController {

    @FXML
    private BorderPane root;

    @FXML
    private ComboBox<String> fontCombo;

    @FXML
    private ComboBox<String> themeCombo;

    @FXML
    private void initialize() {
        setupFontCombo();
        setupThemeCombo();
    }

    // ============================
    // FONT
    // ============================
    private void setupFontCombo() {
        if (fontCombo == null) return;

        // riempi combo
        fontCombo.getItems().setAll(
                AppFont.ATKINSON.getDisplayName(),
                AppFont.COMIC_NEUE.getDisplayName(),
                AppFont.ROBOTO_MONO.getDisplayName()
        );

        // selezione iniziale = font corrente
        AppFont current = FontManager.getCurrentFont();
        fontCombo.getSelectionModel().select(current.getDisplayName());

        // listener
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
    // TEMA
    // ============================
    private void setupThemeCombo() {
        if (themeCombo == null) return;

        // valori user-friendly
        themeCombo.getItems().setAll("Scuro", "Chiaro");

        // selezione iniziale in base al tema corrente
        Theme currentTheme = ThemeManager.getCurrentTheme();
        if (currentTheme == Theme.DARK) {
            themeCombo.getSelectionModel().select("Scuro");
        } else {
            themeCombo.getSelectionModel().select("Chiaro");
        }

        // listener sui cambi di selezione
        themeCombo.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal == null) return;
                    if (root == null) return;

                    Scene scene = root.getScene();
                    if (scene == null) return;

                    Theme selectedTheme =
                            "Scuro".equals(newVal) ? Theme.DARK : Theme.LIGHT;

                    ThemeManager.setTheme(selectedTheme, scene);
                });
    }
}
