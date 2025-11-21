package com.application.canopy.controller;

import com.application.canopy.Navigator;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

public class SettingsController {

    @FXML
    private BorderPane root;

    // Controller del nav incluso (fx:id="nav" -> navController)
    @FXML
    private NavController navController;

    @FXML
    private void initialize() {
        // Se uno dei due è null, il wire non viene fatto e il nav non naviga
        if (root != null && navController != null) {
            Navigator.wire(navController, root, "settings");
        } else {
            System.out.println("DEBUG SettingsController: root o navController è null");
        }
    }
}
