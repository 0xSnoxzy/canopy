package com.application.canopy.controller;

import com.application.canopy.Navigator;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class AppController {

    @FXML private BorderPane root;
    @FXML private NavController navController;       // dall'fx:include
    @FXML private StackPane contentRoot;   // dove mostriamo le pagine

    @FXML
    private void initialize() {
        // Inizializza il Navigator con il contenitore centrale
        Navigator.init(contentRoot);

        // Collega il nav al Navigator
        navController.setOnNavigate(route -> {
            Navigator.show(route);
            navController.setActive(route);
        });

        // Pagina iniziale
        Navigator.show("home");
        navController.setActive("home");
    }

    public BorderPane getRoot() {
        return root;
    }
}
