package com.application.canopy.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

import java.util.function.Consumer;

public class NavController {

    private Consumer<String> onNavigate; // "home", "achievements", "herbarium", "calendar"

    @FXML private ToggleGroup pagesGroup;
    @FXML private ToggleButton btnHome;
    @FXML private ToggleButton btnAchievements;
    @FXML private ToggleButton btnHerbarium;
    @FXML private ToggleButton btnCalendar;

    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    private void fire(String route) {
        if (onNavigate != null) onNavigate.accept(route);
    }

    // Metodi richiamati dai bottoni (onAction in FXML)
    @FXML private void goHome()        { fire("home"); }
    @FXML private void goAchievements(){ fire("achievements"); }
    @FXML private void goHerbarium()   { fire("herbarium"); }
    @FXML private void goCalendar()    { fire("calendar"); }

    /** Chiamalo dalla pagina che include il nav per evidenziare la voce attiva */
    public void setActive(String route) {
        switch (route) {
            case "home"         -> btnHome.setSelected(true);
            case "achievements" -> btnAchievements.setSelected(true);
            case "herbarium"    -> btnHerbarium.setSelected(true);
            case "calendar"     -> btnCalendar.setSelected(true);
            default -> pagesGroup.selectToggle(null);
        }
    }
}
