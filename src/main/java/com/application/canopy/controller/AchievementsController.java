package com.application.canopy.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

import com.application.canopy.Navigator;

public class AchievementsController {
    // Navigazione pagine
    @FXML private BorderPane root;
    @FXML private NavController navController;

    @FXML private FlowPane achievementsFlow;

    @FXML private VBox detailPane;
    @FXML private ImageView detailImage;
    @FXML private Label detailTitle;
    @FXML private Label detailSubtitle;
    @FXML private Label detailProgress;
    @FXML private Label detailStatus;

    private final List<Achievement> data = new ArrayList<>();
    private final List<Region> cards = new ArrayList<>();
    private Node selectedCard = null;

    @FXML
    public void initialize() {

        Navigator.wire(navController, root, "achievements");


        data.add(new Achievement("Botanico Professionista", "Pianta 10 piante diverse", "/images/achievements/botanist.png", 7, 10, false));
        data.add(new Achievement("Custode della Foresta", "Completa 5 sessioni giornaliere", "/images/achievements/guardian.png", 5, 5, true));
        data.add(new Achievement("Seminatore", "Pianta 3 nuove piante", "/images/achievements/seeder.png", 1, 3, false));
        data.add(new Achievement("Coltivatore Attento", "Fai crescere una pianta per 7 giorni", "/images/achievements/water.png", 7, 7, true));

        renderGrid();

        if (!achievementsFlow.getChildren().isEmpty()) {
            selectCard(achievementsFlow.getChildren().get(0));
        }

        achievementsFlow.widthProperty().addListener((obs, oldW, newW) -> resizeCards());
        achievementsFlow.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) newScene.windowProperty().addListener((o2, ow, nw) -> resizeCards());
        });
    }


    private void renderGrid() {
        achievementsFlow.getChildren().clear();
        cards.clear();

        for (Achievement a : data) {
            Region card = (Region) buildCard(a);
            cards.add(card);
            achievementsFlow.getChildren().add(card);
        }
        resizeCards();
    }

    private void resizeCards() {
        if (achievementsFlow == null) return;

        double horizontalPadding = 32.0;
        double gap = achievementsFlow.getHgap();
        double available = Math.max(0, achievementsFlow.getWidth() - horizontalPadding);

        double cardWidth = (available > 0) ? (available - gap) / 2.0 : 360.0;
        cardWidth = Math.max(220, cardWidth);

        for (Region c : cards) {
            c.setPrefWidth(cardWidth);
            c.setMinHeight(Region.USE_PREF_SIZE);
        }
    }

    private Node buildCard(Achievement a) {
        ImageView icon = new ImageView(loadSafe(a.iconPath));
        icon.setFitWidth(48);
        icon.setFitHeight(48);
        icon.setPreserveRatio(true);

        Label title = new Label(a.title);
        title.getStyleClass().add("card-title");

        Label desc = new Label(a.description);
        desc.getStyleClass().add("card-desc");
        desc.setWrapText(true);

        VBox texts = new VBox(4, title, desc);

        HBox card = new HBox(12, icon, texts);
        card.setPadding(new Insets(12));
        card.setCursor(Cursor.HAND);
        card.getStyleClass().addAll("card", a.achieved ? "card-achieved" : "card-locked");

        card.setOnMouseClicked(e -> selectCard(card));
        card.setUserData(a);

        return card;
    }

    private void selectCard(Node card) {
        if (selectedCard != null) selectedCard.getStyleClass().remove("card-selected");
        selectedCard = card;
        if (selectedCard != null) {
            selectedCard.getStyleClass().add("card-selected");
            Achievement a = (Achievement) selectedCard.getUserData();
            updateDetail(a);
        }
    }

    private void updateDetail(Achievement a) {
        detailImage.setImage(loadSafe(a.iconPath));
        detailTitle.setText(a.title);
        detailSubtitle.setText(a.description);
        detailProgress.setText("Progresso: " + a.progress + "/" + a.goal);
        detailStatus.setText(a.achieved ? "Completato" : "Non completato");
        detailPane.getStyleClass().removeAll("detail-achieved", "detail-locked");
        detailPane.getStyleClass().add(a.achieved ? "detail-achieved" : "detail-locked");
    }


    private Image loadSafe(String classpathPath) {
        try (InputStream is = getClass().getResourceAsStream(classpathPath)) {
            if (is != null) {
                return new Image(is);
            } else {
                // immagine trasparente 1x1
                return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGD4DwABBAEAxQY9FwAAAABJRU5ErkJggg==");
            }
        } catch (Exception e) {
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGD4DwABBAEAxQY9FwAAAABJRU5ErkJggg==");
        }
    }

    private static class Achievement {
        final String title;
        final String description;
        final String iconPath;
        final int progress;
        final int goal;
        final boolean achieved;

        Achievement(String title, String description, String iconPath, int progress, int goal, boolean achieved) {
            this.title = title;
            this.description = description;
            this.iconPath = iconPath;
            this.progress = progress;
            this.goal = goal;
            this.achieved = achieved;
        }
    }
}
