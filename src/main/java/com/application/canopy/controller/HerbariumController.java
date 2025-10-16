package com.application.canopy.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
        import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import com.application.canopy.model.Plant;

import java.util.List;

public class HerbariumController {

    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> filterChoice;
    @FXML private GridPane plantGrid;
    @FXML private Label plantName;
    @FXML private TextArea plantDescription;

    private List<Plant> allPlants;

    @FXML
    public void initialize() {
        // categorie base
        filterChoice.getItems().addAll("Tutte", "Fiori", "Alberi", "Erbe");
        filterChoice.setValue("Tutte");

        // dati mock
        allPlants = Plant.samplePlants();

        // listeners (filtri semplici)
        searchField.textProperty().addListener((o, oldV, v) -> render());
        filterChoice.getSelectionModel().selectedItemProperty().addListener((o, oldV, v) -> render());

        // layout griglia in 2 colonne
        render();
    }

    private void render() {
        plantGrid.getChildren().clear();

        String q = (searchField.getText() == null ? "" : searchField.getText()).toLowerCase();
        String cat = filterChoice.getValue();

        List<Plant> filtered = allPlants.stream()
                .filter(p -> p.getName().toLowerCase().contains(q) || p.getDescription().toLowerCase().contains(q))
                .filter(p -> "Tutte".equals(cat) || p.getCategory().equals(cat))
                .toList();

        int col = 0, row = 0;
        for (Plant p : filtered) {
            // placeholder "card": un semplice bottone col nome
            Button card = new Button(p.getName());
            card.setMinWidth(220);
            card.setPrefHeight(60);
            card.setMaxWidth(Double.MAX_VALUE);
            card.getStyleClass().add("plant-card");
            card.setOnAction(e -> showDetails(p));

            plantGrid.add(card, col, row);
            GridPane.setMargin(card, new Insets(4));

            if (++col == 2) { col = 0; row++; }
        }

        // mostra il primo nei dettagli se c'è
        if (!filtered.isEmpty()) showDetails(filtered.getFirst());
        else {
            plantName.setText("Nessun risultato");
            plantDescription.setText("");
        }
    }

    private void showDetails(Plant p) {
        plantName.setText(p.getName());
        plantDescription.setText(p.getDescription());
    }
}
