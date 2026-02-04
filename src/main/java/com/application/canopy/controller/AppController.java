package com.application.canopy.controller;

import com.application.canopy.Navigator;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class AppController {

    @FXML
    private BorderPane root;
    @FXML
    private NavController navController;
    @FXML
    private StackPane contentRoot; // Visualizzatore di tutte le pagine quando caricate

    @FXML
    private void initialize() {
        // Inizializza il Navigator -> nella parte sinistra
        Navigator.init(contentRoot);

        // Collega il nav al Navigator
        navController.setOnNavigate(route -> {
            Navigator.show(route);
            navController.setActive(route);
        });

        // Collega la pagina home
        Navigator.show("home");
        navController.setActive("home");

        // Listener per Focus / Fullscreen Mode
        Navigator.setOnFullScreenToggle(active -> {
            if (navController != null && navController.getView() != null) {
                navController.getView().setVisible(!active);
                navController.getView().setManaged(!active);
            }
        });
    }

    public BorderPane getRoot() {
        return root;
    }
}
