package com.application.canopy.controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

public class NavController {

    // Profile coins
    @FXML private ImageView iconCoins;

    // Menu icons
    @FXML private ImageView iconHome;
    @FXML private ImageView iconAchievements;
    @FXML private ImageView iconHerbarium;
    @FXML private ImageView iconCalendar;

    // Footer icon
    @FXML private ImageView iconLogout;

    @FXML
    public void initialize() {
        // Carica le immagini dal **classpath** (NON file system)
        iconCoins.setImage(load("/com/application/canopy/view/components/images/coins.png"));
        iconHome.setImage(load("/com/application/canopy/view/components/images/home.png"));
        iconAchievements.setImage(load("/com/application/canopy/view/components/images/achievements.png"));
        iconHerbarium.setImage(load("/com/application/canopy/view/components/images/herbarium.png"));
        iconCalendar.setImage(load("/com/application/canopy/view/components/images/calendar.png"));
        iconLogout.setImage(load("/images/icons/logout.png"));
    }

    private Image load(String classpathAbsolutePath) {
        return new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(classpathAbsolutePath),
                "Immagine non trovata nel classpath: " + classpathAbsolutePath
        ));
    }
}
