package com.application.canopy.controller;

import com.application.canopy.model.AchievementGoal;
import com.application.canopy.model.AchievementManager;
import com.application.canopy.model.GameState;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class AchievementsController implements Initializable {

    @FXML private BorderPane root;

    // ---- RIEPILOGO GENERALE ----
    @FXML private StackPane overallRingContainer;
    @FXML private Text overallText;
    @FXML private Text overallHint;

    // ---- LISTA OBIETTIVI ----
    @FXML private FlowPane goalsFlow;

    // ---- DETTAGLI A DESTRA ----
    @FXML private ImageView detailImage;
    @FXML private Region detailImagePlaceholder;
    @FXML private Label detailName;
    @FXML private Text detailShortDescription;
    @FXML private ProgressBar detailProgressBar;
    @FXML private Label detailProgressLabel;
    @FXML private Label detailStatus;

    // ---- MODELLO ----
    private final List<AchievementGoal> goals = new ArrayList<>();
    private final AchievementManager achievementManager = AchievementManager.getInstance();
    private final GameState gameState = GameState.getInstance();

    private RingProgressNode overallRing;
    private HBox selectedCard;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // CSS specifico degli obiettivi
        root.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/achievements.css")).toExternalForm()
        );

        // crea il donut grande nel contenitore overallRingContainer
        overallRing = new RingProgressNode(140, 10);
        overallRing.getLabel().getStyleClass().addAll("ring-label", "ring-label-big");
        overallRingContainer.getChildren().add(overallRing);

        // primo caricamento
        refreshAchievements();

        // 🔁 ogni volta che questo root viene aggiunto a un parent (cioè la pagina viene mostrata)
        // ricalcoliamo gli obiettivi leggendo lo stato attuale del GameState
        root.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                refreshAchievements();
            }
        });
    }

    /**
     * Ricalcola la lista di achievement dal GameState
     * e aggiorna la UI (cards + donut generale).
     * NON tocca il pannello di destra (dettagli) per evitare NPE
     * e lasciarlo gestito dai click sulle card.
     */
    private void refreshAchievements() {
        goals.clear();
        goals.addAll(achievementManager.evaluateAll(gameState));

        buildGoalCards();
        updateOverall();
        // Non chiamiamo showDetails() qui: verrà chiamato solo quando l'utente clicca una card.
    }

    /* ---------- RIEPILOGO GENERALE ---------- */

    private void updateOverall() {
        if (goals.isEmpty()) {
            overallRing.setProgress(0);
            overallText.setText("Non hai ancora obiettivi.");
            overallHint.setText("");
            return;
        }

        long completed = goals.stream().filter(AchievementGoal::isCompleted).count();
        double ratio = (double) completed / goals.size();

        overallRing.setProgress(ratio);

        overallText.setText(
                "Hai completato " + completed + " obiettivi su " + goals.size() + "."
        );
        overallHint.setText("Continua a completare obiettivi!");
    }

    /* ---------- LISTA OBIETTIVI ---------- */

    private void buildGoalCards() {
        goalsFlow.getChildren().clear();
        selectedCard = null;

        for (AchievementGoal g : goals) {
            HBox card = createGoalCard(g);
            goalsFlow.getChildren().add(card);
        }
    }

    private HBox createGoalCard(AchievementGoal goal) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("goal-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // layout responsive:
        // - sotto ~900px: una card per riga (quasi full-width)
        // - sopra ~900px: due card per riga
        card.prefWidthProperty().bind(
                Bindings.when(goalsFlow.widthProperty().greaterThan(900))
                        // finestra larga: 2 per riga
                        .then(goalsFlow.widthProperty().divide(2).subtract(24))
                        // finestra stretta: 1 per riga
                        .otherwise(goalsFlow.widthProperty().subtract(16))
        );

        // donut piccolo per il singolo obiettivo
        RingProgressNode ring = new RingProgressNode(70, 8);
        ring.setProgress(goal.getCompletionRatio());
        ring.getLabel().getStyleClass().addAll("ring-label", "ring-label-small");

        VBox textBox = new VBox(4);
        Label title = new Label(goal.getName());
        title.getStyleClass().add("goal-title");

        Text desc = new Text(goal.getShortDescription());
        desc.setWrappingWidth(220);
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
        if (!selectedCard.getStyleClass().contains("goal-card-selected")) {
            selectedCard.getStyleClass().add("goal-card-selected");
        }
    }

    /* ---------- DETTAGLI A DESTRA ---------- */

    private void showDetails(AchievementGoal goal) {
        if (goal == null) {
            return;
        }

        // Se i controlli di dettaglio non sono stati iniettati (FXML diverso / fx:id mancanti),
        // evitiamo di fare qualsiasi cosa per non avere NullPointerException.
        if (detailName == null ||
                detailShortDescription == null ||
                detailProgressBar == null ||
                detailProgressLabel == null ||
                detailStatus == null ||
                detailImage == null ||
                detailImagePlaceholder == null) {
            return;
        }

        detailName.setText(goal.getName());
        detailShortDescription.setText(goal.getDescription());
        detailProgressBar.setProgress(goal.getCompletionRatio());
        detailProgressLabel.setText(goal.getCurrent() + "/" + goal.getTotal());

        String stato = goal.isCompleted() ? "Completato" : "In corso";
        detailStatus.setText("Stato: " + stato);

        String iconPath = goal.getIconPath();
        if (iconPath != null) {
            try {
                Image img = new Image(getClass().getResourceAsStream(iconPath));
                detailImage.setImage(img);
                detailImage.setVisible(true);
                detailImagePlaceholder.setVisible(false);
            } catch (Exception e) {
                detailImage.setImage(null);
                detailImage.setVisible(false);
                detailImagePlaceholder.setVisible(true);
            }
        } else {
            detailImage.setImage(null);
            detailImage.setVisible(false);
            detailImagePlaceholder.setVisible(true);
        }
    }

    /* ---------- COMPONENTE: DONUT ---------- */

    /**
     * Nodo che disegna un anello di progresso tipo “donut” con percentuale al centro.
     */
    private static class RingProgressNode extends StackPane {

        private final Arc arc;
        private final Label label;

        RingProgressNode(int size, double thickness) {
            setPrefSize(size, size);
            setMinSize(size, size);
            setMaxSize(size, size);

            double radius = size / 2.0;

            Pane draw = new Pane();
            draw.setPrefSize(size, size);
            draw.setMinSize(size, size);
            draw.setMaxSize(size, size);

            // cerchio di fondo
            Circle track = new Circle(radius, radius, radius - thickness / 2);
            track.getStyleClass().add("ring-track");

            // cerchio interno
            Circle inner = new Circle(radius, radius, radius - thickness - 2);
            inner.getStyleClass().add("ring-inner");

            // arco di progresso
            arc = new Arc(
                    radius,
                    radius,
                    radius - thickness / 2,
                    radius - thickness / 2,
                    90,
                    0
            );
            arc.setType(ArcType.OPEN);
            arc.setStrokeLineCap(StrokeLineCap.ROUND);
            arc.getStyleClass().add("ring-progress");

            draw.getChildren().addAll(track, inner, arc);

            label = new Label("0%");
            getChildren().addAll(draw, label);
            setAlignment(label, Pos.CENTER);
        }

        void setProgress(double value) {
            value = Math.max(0, Math.min(1, value));
            arc.setLength(-360 * value);   // senso orario
            int percent = (int) Math.round(value * 100);
            label.setText(percent + "%");
        }

        Label getLabel() {
            return label;
        }
    }
}
