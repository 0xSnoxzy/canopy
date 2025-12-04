package com.application.canopy.controller;

import com.application.canopy.model.GameState;
import com.application.canopy.model.Plant;
import com.application.canopy.model.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;

public class HomeController {

    @FXML private BorderPane root;

    @FXML private Canvas canvas;
    @FXML private ImageView img;
    @FXML private Label lblTimer;
    @FXML private ProgressBar progress;
    @FXML private ProgressBar sessionProgress;

    @FXML private Button btnStartReset;
    @FXML private CheckBox focusMode;

    @FXML private ListView<Plant> list;

    // ----------------- TIMER / STATO POMODORO -----------------

    private Timeline timeline;

    private int focusMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int longBreakInterval = 4; // default, sovrascritto dal dialog

    private int totalSeconds = focusMinutes * 60;
    private int remainingSeconds = totalSeconds;

    private enum TimerState { IDLE, RUNNING }
    private enum Phase { FOCUS, BREAK }

    private TimerState state = TimerState.IDLE;
    private Phase phase = Phase.FOCUS;

    private int totalCycles = 2;      // numero di blocchi di focus
    private int completedCycles = 0;  // quanti blocchi di focus completati

    private boolean breaksEnabled = true; // se false → solo timer, niente pause

    // durata complessiva della sessione (per la barra generale)
    private int sessionTotalSeconds   = 0;
    private int sessionElapsedSeconds = 0;


    // ----------------- MODELLO / GIOCO -----------------

    private final GameState gameState = GameState.getInstance();
    private Plant currentPlant;

    // ----------------- IMMAGINI PIANTE (provvisorio)  -----------------

    private static final String ROOT = "/com/application/canopy/view/components/images/";
    private static final String THUMBS_DIR = ROOT + "thumbs/";
    private static final String PLANTS_DIR = ROOT + "plants/";
    private static final String[] STAGE_FILES = { "stage0.png", "stage1.png", "stage2.png", "stage3.png" };
    private static final String WILT_FILE = "stage0.png";

    private final Image[] frames = new Image[4];
    private Image wiltFrame = null;

    private String currentPlantBasePath = PLANTS_DIR + "Lavanda/";

    @FXML
    private void initialize() {

        // canvas disattivato
        if (canvas != null) {
            canvas.setVisible(false);
            canvas.setManaged(false);
        }

        img.setVisible(true);
        img.setPreserveRatio(true);

        setupPlantList();

        // 👉 Classe CSS per targettare SOLO la lista piante
        if (!list.getStyleClass().contains("plant-list")) {
            list.getStyleClass().add("plant-list");
        }

        // seleziona pianta di default
        if (!list.getItems().isEmpty()) {
            list.getSelectionModel().selectFirst();
            Plant sel = list.getSelectionModel().getSelectedItem();
            setCurrentPlant(sel);
        }

        // focus mode inizialmente nascosto
        focusMode.setVisible(false);
        focusMode.setManaged(false);
        focusMode.selectedProperty().addListener((o, was, is) -> toggleFocus(is));

        btnStartReset.setOnAction(e -> onStartReset());

        // stato iniziale: focus 25 min, timer fermo
        setPhase(Phase.FOCUS, focusMinutes);
        showStage(0);
        updateUI();
        updateButtonUI();
    }


    private void setupPlantList() {
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
                    subtitle.setText(p.getDescription());
                    icon.setImage(loadThumbFor(p));
                    setGraphic(root);
                }
            }
        });

        list.getSelectionModel().selectedItemProperty().addListener((obs, oldP, p) -> {
            if (p != null) setCurrentPlant(p);
        });
    }

    private void setCurrentPlant(Plant p) {
        this.currentPlant = p;
        String folder = p.getFolderName();
        currentPlantBasePath = PLANTS_DIR + folder + "/";
        loadImages();
        showStage(0);
    }

    // ----------------- LOGICA BOTTONE START / RESET -----------------

    private void onStartReset() {
        if (state == TimerState.IDLE) {
            boolean ok = showTimerPopup();
            if (!ok) {
                return;
            }
            startTimer();
        } else {
            resetAndWilt();
        }
    }

    private boolean showTimerPopup() {
        try {
            URL fxml = getClass().getResource("/com/application/canopy/view/timer-dialog.fxml");
            if (fxml == null) {
                System.err.println("[Canopy] timer-dialog.fxml NON trovato!");
                return false;
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent content = loader.load();
            TimerDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Configura timer");

            DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);

            // CSS + tema
            URL css = getClass().getResource("/css/base.css");
            if (css != null) {
                String cssUrl = css.toExternalForm();
                if (!pane.getStylesheets().contains(cssUrl)) {
                    pane.getStylesheets().add(cssUrl);
                }
            }
            if (!pane.getStyleClass().contains("root")) {
                pane.getStyleClass().add("root");
            }
            ThemeManager.applyTheme(pane);
            ThemeManager.addThemeListener(theme -> ThemeManager.applyTheme(pane));

            ButtonType startType = new ButtonType("Avvia", ButtonBar.ButtonData.OK_DONE);
            pane.getButtonTypes().addAll(startType, ButtonType.CANCEL);

            var result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != startType) {
                return false;
            }

            // Leggi scelta finale
            TimerDialogController.Choice choice = controller.getResult();
            if (choice == null) return false;

            if (choice.getType() == TimerDialogController.Choice.Type.PRESET) {
                var p = choice.getPreset();
                if (p == null) return false;

                int macroCycles = Math.max(1, choice.getCycles());          // quante "routine"
                int focusPerMacro = Math.max(1, p.getRepeatBeforeLongBreak()); // es. 3 = F-B-F-B-F-L

                // durata blocchi in secondi
                int focusSec = p.getFocusMinutes()      * 60;
                int shortSec = p.getShortBreakMinutes() * 60;
                int longSec  = p.getLongBreakMinutes()  * 60;

                // salviamo la config per il timer “di blocco”
                focusMinutes      = p.getFocusMinutes();
                shortBreakMinutes = p.getShortBreakMinutes();
                longBreakMinutes  = p.getLongBreakMinutes();

                // totalCycles = numero totale di FOCUS (non di pause)
                totalCycles       = macroCycles * focusPerMacro;
                longBreakInterval = focusPerMacro;  // ogni focusPerMacro focus → pausa lunga
                breaksEnabled     = true;

                // ---------- DURATA COMPLESSIVA SESSIONE ----------
                // Ogni "macro" (routine) = F-B-F-B-...-F + longBreak
                // = focusPerMacro * focusSec + (focusPerMacro - 1) * shortSec + longSec
                // ---------- DURATA COMPLESSIVA SESSIONE ----------
                // numero totale di blocchi di focus
                int totalFocus = macroCycles * focusPerMacro;

                // per ogni "macro" ci sono (focusPerMacro - 1) pause brevi
                int totalShortBreaks = macroCycles * (focusPerMacro - 1);

                // numero di pause lunghe = nCicli - 1
                int totalLongBreaks = Math.max(0, macroCycles - 1);

                sessionTotalSeconds =
                        totalFocus       * focusSec +
                                totalShortBreaks * shortSec +
                                totalLongBreaks  * longSec;
            }
            else {
                // timer singolo via ruota
                int mins = choice.getSingleMinutes();
                focusMinutes      = Math.max(1, mins);
                shortBreakMinutes = 0;
                longBreakMinutes  = 0;
                totalCycles       = 1;
                longBreakInterval = 0;
                breaksEnabled     = false;

                // durata totale = solo il blocco di focus
                sessionTotalSeconds = focusMinutes * 60;
            }

            sessionElapsedSeconds = 0;

            completedCycles = 0;
            setPhase(Phase.FOCUS, focusMinutes);
            updateUI();
            showStage(0);

            return true;


        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void startTimer() {
        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        state = TimerState.RUNNING;
        updateButtonUI();

        focusMode.setVisible(true);
        focusMode.setManaged(true);
    }

    private void resetAndWilt() {
        if (timeline != null) timeline.stop();

        if (phase == Phase.FOCUS && remainingSeconds > 0 && currentPlant != null) {
            gameState.onPomodoroAborted(currentPlant);
        }

        // reset sessione
        completedCycles = 0;
        sessionElapsedSeconds = 0;
        setPhase(Phase.FOCUS, focusMinutes);
        updateUI();

        if (wiltFrame != null) img.setImage(wiltFrame);
        else showStage(0);

        state = TimerState.IDLE;
        updateButtonUI();

        if (focusMode.isSelected()) {
            focusMode.setSelected(false);
            toggleFocus(false);
        }
        focusMode.setVisible(false);
        focusMode.setManaged(false);
    }

    // ----------------- CICLO DI TICK -----------------

    private void tick() {
        remainingSeconds = Math.max(0, remainingSeconds - 1);

        // ogni secondo avanza anche il totale della sessione
        if (sessionTotalSeconds > 0 && sessionElapsedSeconds < sessionTotalSeconds) {
            sessionElapsedSeconds++;
        }

        updateUI();

        if (remainingSeconds > 0) return;

        if (phase == Phase.FOCUS) {
            showStage(3);
            if (currentPlant != null) {
                gameState.onPomodoroCompleted(currentPlant);
            }
            completedCycles++;

            if (!breaksEnabled) {
                stopPomodoroSession();
                return;
            }

            boolean hasMoreCycles = completedCycles < totalCycles;

            if (!hasMoreCycles) {
                stopPomodoroSession();
            } else {
                boolean longBreak = (completedCycles % longBreakInterval == 0);
                int breakMinutes = longBreak ? longBreakMinutes : shortBreakMinutes;
                setPhase(Phase.BREAK, breakMinutes);
                updateUI();
            }

        } else { // BREAK
            boolean hasMoreCycles = completedCycles < totalCycles;

            if (!hasMoreCycles) {
                stopPomodoroSession();
            } else {
                setPhase(Phase.FOCUS, focusMinutes);
                showStage(0);
                updateUI();
            }
        }
    }

    private void stopPomodoroSession() {
        if (timeline != null) timeline.stop();
        state = TimerState.IDLE;

        completedCycles = 0;
        setPhase(Phase.FOCUS, focusMinutes);

        updateButtonUI();

        if (focusMode.isSelected()) {
            focusMode.setSelected(false);
            toggleFocus(false);
        }
        focusMode.setVisible(false);
        focusMode.setManaged(false);
    }

    private void setPhase(Phase newPhase, int minutes) {
        phase = newPhase;
        totalSeconds = Math.max(1, minutes) * 60;
        remainingSeconds = totalSeconds;
    }

    // ----------------- UI HELPERS -----------------

    private void updateUI() {
        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        lblTimer.setText(String.format("%02d:%02d", m, s));

        double p = (totalSeconds == 0) ? 0 : 1.0 - (remainingSeconds / (double) totalSeconds);
        progress.setProgress(p);

        // barra complessiva della sessione
        if (sessionProgress != null) {
            double frac = (sessionTotalSeconds <= 0)
                    ? 0.0
                    : Math.min(1.0, sessionElapsedSeconds / (double) sessionTotalSeconds);

            sessionProgress.setProgress(frac);
        }

        if (state == TimerState.RUNNING && phase == Phase.FOCUS) {
            updateGrowthFrame(p); //la pianta non cresce fuori dai timer di focus
        }
    }

    private void updateButtonUI() {
        boolean running = (state == TimerState.RUNNING);
        btnStartReset.setText(running ? "Reset" : "Start");

        if (running) {
            btnStartReset.getStyleClass().remove("start");
        } else if (!btnStartReset.getStyleClass().contains("start")) {
            btnStartReset.getStyleClass().add("start");
        }

        list.setDisable(running);
        list.setOpacity(running ? 0.6 : 1.0);
    }


    // ----------------- IMMAGINI / CRESCITA -----------------

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

    // ----------------- FOCUS MODE -----------------

    private void toggleFocus(boolean on) {
        Runnable apply = () -> {
            var side = root.getScene().lookup(".sidebar-right");
            if (side != null) {
                side.setVisible(!on);
                side.setManaged(!on);
            }

            var nav = root.getScene().lookup("#navBar");
            if (nav != null) {
                nav.setVisible(!on);
                nav.setManaged(!on);
            }
        };

        if (root.getScene() != null) apply.run();
        else root.sceneProperty().addListener((obs, old, sc) -> apply.run());
    }

    // ----------------- THUMBNAIL PIANTE -----------------

    private Image loadThumbFor(Plant plant) {
        String fileName = plant.getThumbFile();
        String path = THUMBS_DIR + fileName;
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("[Canopy] Thumb NON trovata: " + path);
            return null;
        }
        return new Image(url.toExternalForm(), true);
    }
}
