package com.application.canopy.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainController {
    @FXML private Label welcomeText;
    @FXML private Button miBoton;

    @FXML
    private void Prueba(ActionEvent e) {
        Button b = (Button) e.getSource();
        b.setText(b.getText().equals("Mostrar") ? "Ocultar" : "Mostrar");
    }

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("ANTONIO!!!!");
    }
}
