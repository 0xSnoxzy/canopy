package com.application.canopy.controller;

import com.application.canopy.model.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.function.Consumer;

import static com.application.canopy.model.ThemeManager.Theme;

public class NavController {

    private Consumer<String> onNavigate; // "home", "achievements", "herbarium", "calendar", "settings"

    @FXML private ToggleGroup pagesGroup;
    @FXML private ToggleButton btnHome;
    @FXML private ToggleButton btnAchievements;
    @FXML private ToggleButton btnHerbarium;
    @FXML private ToggleButton btnCalendar;
    @FXML private ToggleButton btnSettings;

    // ---- Icone FXML ----
    @FXML private ImageView homeIcon;
    @FXML private ImageView achievementsIcon;
    @FXML private ImageView herbariumIcon;
    @FXML private ImageView calendarIcon;
    @FXML private ImageView settingsIcon;

    // ========================================================
    //  INIT — viene chiamato automaticamente dal FXMLLoader
    // ========================================================
    @FXML
    private void initialize() {
        // mi iscrivo ai cambi tema
        ThemeManager.addThemeListener(this::updateIconsForTheme);
        // non serve più chiamare getCurrentTheme() manualmente:
        // addThemeListener chiama subito updateIconsForTheme col tema corrente
    }

    // ========================================================
    //  Aggiorna icone in base al tema corrente
    // ========================================================
    private void updateIconsForTheme(Theme theme) {
        String base = "/com/application/canopy/view/components/images/";

        if (theme == Theme.LIGHT) {
            // TEMA CHIARO → icone nere (i tuoi .png normali)
            homeIcon.setImage(new Image(getClass().getResourceAsStream(base + "home.png")));
            achievementsIcon.setImage(new Image(getClass().getResourceAsStream(base + "achievements.png")));
            herbariumIcon.setImage(new Image(getClass().getResourceAsStream(base + "herbarium.png")));
            calendarIcon.setImage(new Image(getClass().getResourceAsStream(base + "calendar.png")));
            settingsIcon.setImage(new Image(getClass().getResourceAsStream(base + "settings.png")));
        } else {
            // TEMA SCURO → icone bianche (i tuoi *-dark che hai detto essere bianchi)
            homeIcon.setImage(new Image(getClass().getResourceAsStream(base + "home-dark.png")));
            achievementsIcon.setImage(new Image(getClass().getResourceAsStream(base + "achievements-dark.png")));
            herbariumIcon.setImage(new Image(getClass().getResourceAsStream(base + "herbarium-dark.png")));
            calendarIcon.setImage(new Image(getClass().getResourceAsStream(base + "calendar-dark.png")));
            settingsIcon.setImage(new Image(getClass().getResourceAsStream(base + "settings-dark.png")));
        }
    }

    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    private void fire(String route) {
        if (onNavigate != null) onNavigate.accept(route);
    }

    // Metodi richiamati dai bottoni
    @FXML private void goHome()        { fire("home"); }
    @FXML private void goAchievements(){ fire("achievements"); }
    @FXML private void goHerbarium()   { fire("herbarium"); }
    @FXML private void goCalendar()    { fire("calendar"); }
    @FXML private void goSettings()    { fire("settings"); }

    /** Evidenzia la voce attiva */
    public void setActive(String route) {
        switch (route) {
            case "home"         -> btnHome.setSelected(true);
            case "achievements" -> btnAchievements.setSelected(true);
            case "herbarium"    -> btnHerbarium.setSelected(true);
            case "calendar"     -> btnCalendar.setSelected(true);
            case "settings"     -> btnSettings.setSelected(true);
            default             -> pagesGroup.selectToggle(null);
        }
    }
}
