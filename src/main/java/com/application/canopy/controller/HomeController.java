package com.application.canopy.controller;

import com.application.canopy.Navigator;
import com.application.canopy.model.GameState;
import com.application.canopy.model.Plant;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;

public class HomeController {

    @FXML private BorderPane root;
    @FXML private NavController navController;

    @FXML private Canvas canvas;
    @FXML private ImageView img;
    @FXML private Label lblTimer;
    @FXML private ProgressBar progress;

    @FXML private Button btnStartReset;
    @FXML private ComboBox<String> sessionCombo;
    @FXML private TextField customMinutes;
    @FXML private CheckBox focusMode;

    @FXML private ListView<Plant> list;

    // timer
    private Timeline timeline;
    private int totalSeconds = 25 * 60;
    private int remainingSeconds = totalSeconds;

    private enum TimerState { IDLE, RUNNING }
    private TimerState state = TimerState.IDLE;

    // modello / stato globale
    private final GameState gameState = GameState.getInstance();
    private Plant currentPlant; // pianta attualmente selezionata nella home

    // path immagini
    private static final String ROOT = "/com/application/canopy/view/components/images/";
    private static final String THUMBS_DIR = ROOT + "thumbs/";
    private static final String PLANTS_DIR = ROOT + "plants/";
    private static final String[] STAGE_FILES = { "stage0.png", "stage1.png", "stage2.png", "stage3.png" };
    private static final String WILT_FILE = "stage0.png";

    private final Image[] frames = new Image[4];
    private Image wiltFrame = null;

    // path di default (verrà sovrascritto da setCurrentPlant)
    private String currentPlantBasePath = PLANTS_DIR + "Lavanda/";

    @FXML
    private void initialize() {
        Navigator.wire(navController, root, "home");

        // canvas disattivato
        if (canvas != null) {
            canvas.setVisible(false);
            canvas.setManaged(false);
        }

        img.setVisible(true);
        img.setPreserveRatio(true);

        setupPlantList();

        // seleziona pianta di default (prima lista) e carica gli stage
        if (!list.getItems().isEmpty()) {
            list.getSelectionModel().selectFirst();
            Plant sel = list.getSelectionModel().getSelectedItem();
            setCurrentPlant(sel);
        }

        btnStartReset.setOnAction(e -> onStartReset());
        focusMode.selectedProperty().addListener((o, was, is) -> toggleFocus(is));

        // preset timer
        sessionCombo.getSelectionModel().select(0);
        sessionCombo.valueProperty().addListener((o, old, v) -> applyPreset(v));
        applyPreset(sessionCombo.getValue());

        showStage(0);
        updateUI();
        updateButtonUI();
    }

    private void setupPlantList() {
        // usa il catalogo dal modello (coincide col GameState)
        list.getItems().setAll(Plant.samplePlants());

        list.setCellFactory(v -> new ListCell<>() {
            private final ImageView icon = new ImageView();
            private final Label title = new Label();
            private final Label subtitle = new Label();
            private final Label description = new Label();
            private final VBox texts = new VBox(2, title, subtitle, description);
            private final HBox root = new HBox(10, icon, texts);
            {
                setPrefWidth(0);
                icon.setFitWidth(72);
                icon.setFitHeight(72);
                icon.setPreserveRatio(true);
                title.setStyle("-fx-font-weight: 800;");
                description.getStyleClass().add("subtle");
                HBox.setHgrow(texts, Priority.ALWAYS);
            }
            @Override
            protected void updateItem(Plant p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                } else {
                    title.setText(p.getName());
                    subtitle.setText(p.getLatinName());
                    description.setText(p.getDescription());
                    icon.setImage(loadThumbFor(p));
                    setGraphic(root);
                }
            }
        });

        // cambio pianta
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldP, p) -> {
            if (p != null) setCurrentPlant(p);
        });
    }

    // imposta base path in base alla pianta scelta e carica gli stage
    private void setCurrentPlant(Plant p) {
        this.currentPlant = p;
        String folder = p.getFolderName(); // -> es: "Lavanda"
        currentPlantBasePath = PLANTS_DIR + folder + "/";
        loadImages();
        showStage(0);
    }

    // --- TIMER -------------------------------------------------------------

    private void onStartReset() {
        if (state == TimerState.IDLE) {
            startTimer();
        } else {
            resetAndWilt(); // interrompe → pianta "muore"
        }
    }

    private void startTimer() {
        try {
            if (!customMinutes.getText().isBlank()) {
                int m = Integer.parseInt(customMinutes.getText().trim());
                setDurationMinutes(m);
            }
        } catch (NumberFormatException ignored) {}

        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        state = TimerState.RUNNING;
        updateButtonUI();
    }

    private void tick() {
        remainingSeconds = Math.max(0, remainingSeconds - 1);
        updateUI();

        if (remainingSeconds == 0) {
            if (timeline != null) timeline.stop();
            showStage(3); // forma finale

            // QUI colleghiamo la home al modello di gioco -----------------
            if (currentPlant != null) {
                gameState.onPomodoroCompleted(currentPlant);
            }

            state = TimerState.IDLE;
            updateButtonUI();
        }
    }

    private void resetAndWilt() {
        if (timeline != null) timeline.stop();

        // reset durante RUNNING = pomodoro interrotto → pianta "muore"
        if (currentPlant != null) {
            gameState.onPomodoroAborted(currentPlant);
        }

        remainingSeconds = totalSeconds;
        updateUI();
        if (wiltFrame != null) img.setImage(wiltFrame);
        else showStage(0);

        state = TimerState.IDLE;
        updateButtonUI();
    }

    // --- UI helpers --------------------------------------------------------

    private void updateUI() {
        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        lblTimer.setText(String.format("%02d:%02d", m, s));

        double p = (totalSeconds == 0) ? 0 : 1.0 - (remainingSeconds / (double) totalSeconds);
        progress.setProgress(p);

        if (state == TimerState.RUNNING) updateGrowthFrame(p);
    }

    private void updateButtonUI() {
        boolean running = (state == TimerState.RUNNING);
        btnStartReset.setText(running ? "Reset" : "Start");

        if (running) {
            btnStartReset.getStyleClass().remove("start");
        } else if (!btnStartReset.getStyleClass().contains("start")) {
            btnStartReset.getStyleClass().add("start");
        }

        sessionCombo.setDisable(running || focusMode.isSelected());
        customMinutes.setDisable(running || focusMode.isSelected());

        // blocca cambio pianta mentre il timer è attivo
        list.setDisable(running);
        list.setOpacity(running ? 0.6 : 1.0);
    }

    private void applyPreset(String label) {
        int m = 25;
        if (label != null) {
            if (label.contains("Breve")) m = 5;
            else if (label.contains("Lunga")) m = 15;
        }
        customMinutes.clear();
        setDurationMinutes(m);
        resetTimerOnly();
    }

    private void setDurationMinutes(int minutes) {
        totalSeconds = Math.max(1, minutes) * 60;
        remainingSeconds = totalSeconds;
        showStage(0);
        updateUI();
    }

    private void resetTimerOnly() {
        if (timeline != null) timeline.stop();
        remainingSeconds = totalSeconds;
        showStage(0);
        state = TimerState.IDLE;
        updateUI();
        updateButtonUI();
    }

    // --- growth / immagini -------------------------------------------------

    private void loadImages() {
        for (int i = 0; i < STAGE_FILES.length; i++) {
            frames[i] = loadImage(currentPlantBasePath + STAGE_FILES[i]);
        }
        wiltFrame = loadImage(currentPlantBasePath + WILT_FILE);

        img.setVisible(true);
        img.setPreserveRatio(true);
    }

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("[Canopy] Immagine NON trovata: " + path);
            return null;
        }
        return new Image(url.toExternalForm(), true);
    }

    private void updateGrowthFrame(double p) {
        int stage = (p >= 2.0 / 3.0) ? 2 : (p >= 1.0 / 3.0 ? 1 : 0);
        showStage(stage);
    }

    private void showStage(int idx) {
        if (idx < 0 || idx >= frames.length) idx = 0;
        Image target = frames[idx];

        if (target == null) {
            for (int i = idx; i >= 0; i--) {
                if (frames[i] != null) { target = frames[i]; break; }
            }
            if (target == null) {
                for (int i = idx; i < frames.length; i++) {
                    if (frames[i] != null) { target = frames[i]; break; }
                }
            }
        }

        if (target != null) img.setImage(target);
    }

    // --- focus mode --------------------------------------------------------

    private void toggleFocus(boolean on) {
        boolean running = (state == TimerState.RUNNING);
        sessionCombo.setDisable(on || running);
        customMinutes.setDisable(on || running);

        Runnable apply = () -> {
            var side = img.getScene().lookup(".side-panel");
            if (side != null) {
                side.setVisible(!on);
                side.setManaged(!on);
            }
            var nav = img.getScene().lookup("#navBar");
            if (nav != null) {
                nav.setVisible(!on);
                nav.setManaged(!on);
            }
        };

        if (img.getScene() != null) apply.run();
        else img.sceneProperty().addListener((obs, old, sc) -> apply.run());
    }

    // thumbs piante (ora usa Plant.getThumbFile)
    private Image loadThumbFor(Plant plant) {
        String fileName = plant.getThumbFile(); // es: "Lavanda.png"
        String path = THUMBS_DIR + fileName;
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("[Canopy] Thumb NON trovata: " + path);
            return null;
        }
        return new Image(url.toExternalForm(), true);
    }
}
