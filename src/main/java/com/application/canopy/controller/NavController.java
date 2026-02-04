package com.application.canopy.controller;

import com.application.canopy.model.GameState;
import com.application.canopy.model.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.function.Consumer;

public class NavController {

    private Consumer<String> onNavigate; // "home", "achievements", "herbarium", "calendar", "settings"

    @FXML
    private Label userPoints;

    @FXML
    private ToggleGroup pagesGroup;
    @FXML
    private ToggleButton btnHome;
    @FXML
    private ToggleButton btnAchievements;
    @FXML
    private ToggleButton btnHerbarium;
    @FXML
    private ToggleButton btnCalendar;
    @FXML
    private javafx.scene.layout.VBox navBar;
    @FXML
    private ToggleButton btnSettings;

    public javafx.scene.Node getView() {
        return navBar;
    }

    // Icone
    @FXML
    private ImageView homeIcon;
    @FXML
    private ImageView achievementsIcon;
    @FXML
    private ImageView herbariumIcon;
    @FXML
    private ImageView calendarIcon;
    @FXML
    private ImageView settingsIcon;
    // @FXML private ImageView streakIcon;

    private String currentThemeId = "dark";

    private Image homeBlack;
    private Image homeWhite;
    private Image achievementsBlack;
    private Image achievementsWhite;
    private Image herbariumBlack;
    private Image herbariumWhite;
    private Image calendarBlack;
    private Image calendarWhite;
    private Image settingsBlack;
    private Image settingsWhite;

    @FXML
    private void initialize() {
        cacheIcons();

        // Listener che chiama onThemeChanged quando il tema cambia
        ThemeManager.addThemeListener(this::onThemeChanged);

        // Aggiorna le icone quando viene triggerato il listener
        installSelectionListeners();

        // Aggiorna le icone al primo avvio
        onThemeChanged(currentThemeId);

        // Aggiorna la streak
        GameState.getInstance().addStatsListener(this::refreshStreak);
        refreshStreak();
    }

    private void refreshStreak() {
        int streak = GameState.getInstance().getGlobalStreak();
        Platform.runLater(() -> {
            if (userPoints != null) {
                userPoints.setText("🌿 " + streak);
            }
        });
    }

    private void cacheIcons() {
        // Carica icone NERE
        homeBlack = com.application.canopy.util.ResourceManager.getNavIcon("home", false);
        achievementsBlack = com.application.canopy.util.ResourceManager.getNavIcon("achievements", false);
        herbariumBlack = com.application.canopy.util.ResourceManager.getNavIcon("herbarium", false);
        calendarBlack = com.application.canopy.util.ResourceManager.getNavIcon("calendar", false);
        settingsBlack = com.application.canopy.util.ResourceManager.getNavIcon("settings", false);

        // Carica icone BIANCHE
        homeWhite = com.application.canopy.util.ResourceManager.getNavIcon("home", true);
        achievementsWhite = com.application.canopy.util.ResourceManager.getNavIcon("achievements", true);
        herbariumWhite = com.application.canopy.util.ResourceManager.getNavIcon("herbarium", true);
        calendarWhite = com.application.canopy.util.ResourceManager.getNavIcon("calendar", true);
        settingsWhite = com.application.canopy.util.ResourceManager.getNavIcon("settings", true);
    }

    private void onThemeChanged(String themeId) {
        if (themeId == null || themeId.isBlank()) {
            themeId = "dark";
        }
        this.currentThemeId = themeId.toLowerCase().trim();
        updateIconsForThemeAndSelection();
    }

    private void installSelectionListeners() {
        addSelectionListener(btnHome);
        addSelectionListener(btnAchievements);
        addSelectionListener(btnHerbarium);
        addSelectionListener(btnCalendar);
        addSelectionListener(btnSettings);
    }

    private void addSelectionListener(ToggleButton button) {
        button.selectedProperty().addListener((obs, oldVal, newVal) -> updateIconsForThemeAndSelection());
    }

    /**
     * Applica le regole:
     * - tutti i temi SCURI tranne evergreen ("dark") e daltonici:
     * SOLO la pagina selezionata -> icona NERA
     * - tutti i temi CHIARI tranne evergreen ("light") e daltonici:
     * SOLO la pagina selezionata -> icona BIANCA
     * - evergreen + daltonici: comportamento classico (icone tutte chiare su dark,
     * tutte scure su light)
     */
    private void updateIconsForThemeAndSelection() {
        String id = currentThemeId;

        boolean isEvergreen = "dark".equals(id) || "light".equals(id);
        boolean isDaltonici = id.contains("daltonici");
        boolean isDarkTheme = "dark".equals(id) || id.endsWith("-dark");
        boolean isLightTheme = "light".equals(id) || id.endsWith("-light");

        if (isEvergreen || isDaltonici) {
            // comportamento "classico":
            if (isDarkTheme) {
                // nav con testo chiaro -> icone chiare
                setAllIconsWhite();
            } else {
                // nav con testo scuro -> icone scure
                setAllIconsBlack();
            }
            return;
        }

        // Qui siamo nei temi "colorati" (sakura, quercia, menta, peperoncino, lavanda,
        // orchidea, ecc.)

        if (isDarkTheme) {
            // Tema SCURO non evergreen/non daltonici:
            // - bottoni NON selezionati: icona bianca
            // - bottone selezionato: icona nera
            homeIcon.setImage(btnHome.isSelected() ? homeBlack : homeWhite);
            achievementsIcon.setImage(btnAchievements.isSelected() ? achievementsBlack : achievementsWhite);
            herbariumIcon.setImage(btnHerbarium.isSelected() ? herbariumBlack : herbariumWhite);
            calendarIcon.setImage(btnCalendar.isSelected() ? calendarBlack : calendarWhite);
            settingsIcon.setImage(btnSettings.isSelected() ? settingsBlack : settingsWhite);
        } else if (isLightTheme) {
            // Tema CHIARO non evergreen/non daltonici:
            // - bottoni NON selezionati: icona nera
            // - bottone selezionato: icona bianca
            homeIcon.setImage(btnHome.isSelected() ? homeWhite : homeBlack);
            achievementsIcon.setImage(btnAchievements.isSelected() ? achievementsWhite : achievementsBlack);
            herbariumIcon.setImage(btnHerbarium.isSelected() ? herbariumWhite : herbariumBlack);
            calendarIcon.setImage(btnCalendar.isSelected() ? calendarWhite : calendarBlack);
            settingsIcon.setImage(btnSettings.isSelected() ? settingsWhite : settingsBlack);
        } else {
            // fallback improbabile → icone bianche su sfondo presumibilmente scuro
            setAllIconsWhite();
        }
    }

    private void setAllIconsBlack() {
        homeIcon.setImage(homeBlack);
        achievementsIcon.setImage(achievementsBlack);
        herbariumIcon.setImage(herbariumBlack);
        calendarIcon.setImage(calendarBlack);
        settingsIcon.setImage(settingsBlack);
    }

    private void setAllIconsWhite() {
        homeIcon.setImage(homeWhite);
        achievementsIcon.setImage(achievementsWhite);
        herbariumIcon.setImage(herbariumWhite);
        calendarIcon.setImage(calendarWhite);
        settingsIcon.setImage(settingsWhite);
    }

    // ===== Navigazione =====

    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    private void fire(String route) {
        if (onNavigate != null)
            onNavigate.accept(route);
    }

    @FXML
    private void goHome() {
        fire("home");
    }

    @FXML
    private void goAchievements() {
        fire("achievements");
    }

    @FXML
    private void goHerbarium() {
        fire("herbarium");
    }

    @FXML
    private void goCalendar() {
        fire("calendar");
    }

    @FXML
    private void goSettings() {
        fire("settings");
    }

    public void setActive(String route) {
        switch (route) {
            case "home" -> btnHome.setSelected(true);
            case "achievements" -> btnAchievements.setSelected(true);
            case "herbarium" -> btnHerbarium.setSelected(true);
            case "calendar" -> btnCalendar.setSelected(true);
            case "settings" -> btnSettings.setSelected(true);
            default -> pagesGroup.selectToggle(null);
        }
        // dopo aver cambiato il selezionato, riallineo le icone
        updateIconsForThemeAndSelection();
    }
}
