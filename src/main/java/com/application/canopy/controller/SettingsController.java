package com.application.canopy.controller;

import com.application.canopy.model.FontManager;
import com.application.canopy.model.FontManager.AppFont;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;

public class SettingsController {

    @FXML
    private BorderPane root;

    @FXML
    private ComboBox<String> fontCombo;

    @FXML
    private void initialize() {
        setupFontCombo();
    }

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
}
