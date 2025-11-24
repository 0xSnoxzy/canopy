package com.application.canopy.controller;

import com.application.canopy.Navigator;
import com.application.canopy.model.FontManager;
import com.application.canopy.model.FontManager.AppFont;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;

public class SettingsController {

    @FXML
    private BorderPane root;

    @FXML
    private NavController navController;

    @FXML
    private ComboBox<String> fontCombo;

    @FXML
    private void initialize() {
        // collega nav e pagina
        if (root != null && navController != null) {
            Navigator.wire(navController, root, "settings");
        }

        // inizializza combo font
        setupFontCombo();
    }

    private void setupFontCombo() {
        if (fontCombo == null) return;

        // riempi combo con i nomi da mostrare
        fontCombo.getItems().setAll(
                AppFont.ATKINSON.getDisplayName(),
                AppFont.COMIC_NEUE.getDisplayName(),
                AppFont.ROBOTO_MONO.getDisplayName()
        );

        // selezione iniziale = font corrente
        AppFont current = FontManager.getCurrentFont();
        fontCombo.getSelectionModel().select(current.getDisplayName());

        // listener cambi
        fontCombo.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal == null) return;
                    AppFont selected = AppFont.fromDisplayName(newVal);
                    FontManager.setFont(selected, root);
                });
    }
}
