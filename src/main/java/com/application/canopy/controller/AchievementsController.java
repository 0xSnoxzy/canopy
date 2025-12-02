package com.application.canopy.controller;

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
    @FXML private StackPane overallRingContainer; // StackPane vacío del FXML
    @FXML private Text overallText;
    @FXML private Text overallHint;
    @FXML private HBox summaryBox;

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

    // dati demo (sostituisci dopo con il tuo modello reale)
    private final List<Goal> goals = new ArrayList<>();

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

        // il testo del riepilogo si adatta allo spazio disponibile
        overallText.wrappingWidthProperty().bind(
                summaryBox.widthProperty()
                        .subtract(overallRingContainer.widthProperty())
                        .subtract(48)
        );
        overallHint.wrappingWidthProperty().bind(
                summaryBox.widthProperty()
                        .subtract(overallRingContainer.widthProperty())
                        .subtract(48)
        );

        // per ora: obiettivi finti di esempio
        loadMockGoals();

        buildGoalCards();
        updateOverall();
    }

    /* ---------- DATI DEMO (puoi sostituire con il tuo modello) ---------- */

    private void loadMockGoals() {
        goals.clear();
        goals.add(new Goal("Botanico Professionista",
                "Pianta 10 piante diverse.", 7, 10, null));
        goals.add(new Goal("Custode della Foresta",
                "Completa 5 sessioni giornaliere.", 3, 5, null));
        goals.add(new Goal("Seminatore",
                "Pianta 3 nuove piante.", 3, 3, null));
        goals.add(new Goal("Coltivatore Attento",
                "Fai crescere una pianta per 7 giorni.", 2, 7, null));
    }

    /* ---------- RIEPILOGO GENERALE ---------- */

    private void updateOverall() {
        if (goals.isEmpty()) {
            overallRing.setProgress(0);
            overallText.setText("Non hai ancora obiettivi.");
            overallHint.setText("");
            return;
        }

        long completed = goals.stream().filter(Goal::isCompleted).count();
        double ratio = (double) completed / goals.size();

        overallRing.setProgress(ratio);

        overallText.setText(
                "Hai completato " + completed + " obiettivi su " + goals.size() + "."
        );
        overallHint.setText("Continua a completare obiettivi per far crescere il tuo giardino!");
    }

    /* ---------- LISTA OBIETTIVI ---------- */

    private void buildGoalCards() {
        goalsFlow.getChildren().clear();

        for (Goal g : goals) {
            HBox card = createGoalCard(g);
            goalsFlow.getChildren().add(card);
        }
    }

    private HBox createGoalCard(Goal goal) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("goal-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // donut piccolo per il singolo obiettivo
        RingProgressNode ring = new RingProgressNode(70, 8);
        ring.setProgress(goal.getCompletionRatio());
        ring.getLabel().getStyleClass().addAll("ring-label", "ring-label-small");

        VBox textBox = new VBox(4);
        Label title = new Label(goal.name);
        title.getStyleClass().add("goal-title");

        Text desc = new Text(goal.shortDescription);
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

    private void showDetails(Goal goal) {
        detailName.setText(goal.name);
        detailShortDescription.setText(goal.description);
        detailProgressBar.setProgress(goal.getCompletionRatio());
        detailProgressLabel.setText(goal.current + "/" + goal.total);

        String stato = goal.isCompleted() ? "Completato" : "In corso";
        detailStatus.setText("Stato: " + stato);

        if (goal.iconPath != null) {
            try {
                Image img = new Image(getClass().getResourceAsStream(goal.iconPath));
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

    /* ---------- MODELLO INTERNO SEMPLICE ---------- */

    private static class Goal {
        final String name;
        final String shortDescription;
        final String description;
        final int current;
        final int total;
        final String iconPath;

        Goal(String name, String description, int current, int total, String iconPath) {
            this.name = name;
            this.shortDescription = description;
            this.description = description;
            this.current = current;
            this.total = total;
            this.iconPath = iconPath;
        }

        double getCompletionRatio() {
            return total <= 0 ? 0 : Math.min(1.0, (double) current / total);
        }

        boolean isCompleted() {
            return total > 0 && current >= total;
        }
    }

    /* ---------- COMPONENTE: DONUT ---------- */

    /** Nodo che disegna un anello di progresso tipo “donut” con percentuale al centro. */
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
            arc = new Arc(radius, radius,
                    radius - thickness / 2,
                    radius - thickness / 2,
                    90, 0);
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
