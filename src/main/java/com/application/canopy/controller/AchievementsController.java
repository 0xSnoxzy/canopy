package com.application.canopy.controller;

import com.application.canopy.model.AchievementGoal;
import com.application.canopy.model.AchievementManager;
import com.application.canopy.model.GameState;
import com.application.canopy.view.components.RingProgressIndicator;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class AchievementsController implements Initializable {

    @FXML
    private BorderPane root;

    // Riepilogo generale
    @FXML
    private StackPane overallRingContainer;
    @FXML
    private Text overallText;
    @FXML
    private Text overallHint;

    // Lista obiettivi
    @FXML
    private FlowPane goalsFlow;

    // Dettagli obiettivo
    @FXML
    private Label detailName;
    @FXML
    private Text detailShortDescription;
    @FXML
    private ProgressBar detailProgressBar;
    @FXML
    private Label detailProgressLabel;
    @FXML
    private Label detailStatus;

    // Modello Anello
    private final List<AchievementGoal> goals = new ArrayList<>();
    private final AchievementManager achievementManager = AchievementManager.getInstance();
    private final GameState gameState = GameState.getInstance();

    private RingProgressIndicator overallRing;
    private HBox selectedCard;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        root.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/achievements.css")).toExternalForm());

        overallRing = new RingProgressIndicator(140, 10);
        overallRing.getLabel().getStyleClass().addAll("ring-label", "ring-label-big");
        overallRingContainer.getChildren().setAll(overallRing);

        //questo porta tutto in scena
        refreshAchievements();

        // Listener che aggiorna il achivements appena viene caricato, se il newParent è
        // il achivements allora aggiorna dal DB per visualizzare
        // Eventuali cambi in background mentre si era in altre pagine
        root.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                refreshAchievements();
            }
        });
    }

    //valuta tutti gli obiettivi sullo stato corrente del db
    private void refreshAchievements() {
        goals.clear();
        goals.addAll(achievementManager.evaluateAll(gameState));

        buildGoalCards(); //ricostruisce le card della lista
        updateOverall(); //aggiorna il anello grande
        resetDetailsPanel(); //azzera il pannello dettagli finché non clicchi una card
    }

    //mette tutti i detagli per default, senza selezionare niente
    private void resetDetailsPanel() {
        detailName.setText("Seleziona un obiettivo");
        detailShortDescription.setText("");
        detailProgressBar.setProgress(0);
        detailProgressLabel.setText("0/0");
        detailStatus.setText("Stato: -");
    }

    //calcola le percentuali dei obbiettivi completati, e aggiorna il cerchio grande
    private void updateOverall() {
        if (goals.isEmpty()) { //se non ce nessun obbiettivo completato al momento, mette tutto per default
            overallRing.setProgress(0);
            overallText.setText("Non hai ancora obiettivi.");
            overallHint.setText("");
            return;
        }

        long completed = goals.stream().filter(AchievementGoal::isCompleted).count();//quanti completati
        double ratio = (double) completed / goals.size();//percentuale, n completati / totali

        overallRing.setProgress(ratio);
        overallText.setText("Hai completato " + completed + " obiettivi su " + goals.size() + ".");
        overallHint.setText("Continua a completare obiettivi!");
    }

    // Lista obiettivi

    private void buildGoalCards() { //Svuota il FlowPane e aggiunge una card per ogni obiettivo
        goalsFlow.getChildren().clear();
        selectedCard = null;

        for (AchievementGoal g : goals) {
            goalsFlow.getChildren().add(createGoalCard(g));
        }
    }

    private HBox createGoalCard(AchievementGoal goal) {//crea una cart obbitivo in una hbox composta dal cerchio + vbox con le info
        HBox card = new HBox(12);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("goal-card");

        card.prefWidthProperty().bind(
                //resize responsivo con binding -> binding javafx, le dimesioni si aggiornano da sole quando si cambia la dimezione
                Bindings.when(goalsFlow.widthProperty().greaterThan(900))//se la lista è larga > 900 →
                        .then(goalsFlow.widthProperty().divide(2).subtract(24))// due colonne
                        .otherwise(goalsFlow.widthProperty().subtract(16)));
        //crea il mini cerchio per ogni obbietivo
        RingProgressIndicator ring = new RingProgressIndicator(70, 8);
        ring.setProgress(goal.getCompletionRatio());
        ring.getLabel().getStyleClass().addAll("ring-label", "ring-label-small");
        //crea la parte con i detagli del obbietivo
        VBox textBox = new VBox(4);
        Label title = new Label(goal.getName());
        title.getStyleClass().add("goal-title");

        Text desc = new Text(goal.getShortDescription());
        desc.setWrappingWidth(220); //limita la larghezza del testo della descrizione, se è lunga va a capo
        desc.getStyleClass().add("goal-description");

        textBox.getChildren().addAll(title, desc);
        card.getChildren().addAll(ring, textBox);

        card.setOnMouseClicked(e -> {
            selectCard(card);
            showDetails(goal);
        });

        return card;
    }

    private void selectCard(HBox card) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("goal-card-selected");
        }
        selectedCard = card;
        selectedCard.getStyleClass().add("goal-card-selected");
    }

    // Mostra obiettivo

    private void showDetails(AchievementGoal goal) {
        detailName.setText(goal.getName());
        detailShortDescription.setText(goal.getDescription());
        detailProgressBar.setProgress(goal.getCompletionRatio());
        detailProgressLabel.setText(goal.getCurrent() + "/" + goal.getTotal());
        detailStatus.setText("Stato: " + (goal.isCompleted() ? "Completato" : "In corso"));
    }
}
