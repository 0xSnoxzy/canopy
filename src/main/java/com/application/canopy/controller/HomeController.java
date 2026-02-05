package com.application.canopy.controller;

import com.application.canopy.db.PlantActivityRepository;
import com.application.canopy.model.GameState;
import com.application.canopy.model.Plant;
import com.application.canopy.model.ThemeManager;
import com.application.canopy.service.PomodoroTimerService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;

public class HomeController {

    @FXML
    private BorderPane root;

    @FXML
    private ImageView img;
    @FXML
    private Label lblTimer;

    // anello timer
    @FXML
    private Circle timerBackground;
    @FXML
    private Arc timerArc;
    @FXML
    private Circle timerKnob;
    @FXML
    private StackPane timerContainer;

    private boolean isDraggingInfo = false;
    private static final int MAX_MINUTES = 120; // 2 ore max da ghiera
    private static final double RADIUS = 250.0; // deve corrispondere all'FXML

    @FXML
    private ProgressBar sessionProgress;

    @FXML
    private Button btnStartReset;
    @FXML
    private Button btnConfig;
    @FXML
    private CheckBox focusMode;
    @FXML
    private VBox rightPanel;

    @FXML
    private ListView<Plant> list;

    // servizi/modello
    private final GameState gameState = GameState.getInstance();
    private final PomodoroTimerService timerService = new PomodoroTimerService();
    private PlantActivityRepository activityRepository;

    private Plant currentPlant;

    // IMMAGINI PIANTE (provvisorio)

    private static final String[] STAGE_FILES = { "stage0.png", "stage1.png", "stage2.png", "stage3.png" };

    private final Image[] frames = new Image[4];
    private Image wiltFrame = null;

    @FXML
    private void initialize() {

        // inizializza repository via Locator
        activityRepository = com.application.canopy.service.ServiceLocator.getInstance().getPlantActivityRepository();

        //configurazione iniziale di default (25 min)
        timerService.configureSingleTimer(25);

        //listener eventi timer
        timerService.setOnPomodoroCompleted(this::onPomodoroCompleted);

        // binding UI agli stati del service
        timerService.remainingSecondsProperty().addListener((o, old, val) -> updateTimerLabel(val.intValue()));
        timerService.timerStateProperty().addListener((o, old, state) -> updateButtonUI(state));

        // listener per progresso barra
        timerService.remainingSecondsProperty().addListener((o, old, val) -> {
            updateProgressBar();
            // aggiorna anche la pianta se in running
            if (timerService.getTimerState() == PomodoroTimerService.TimerState.RUNNING &&
                    timerService.getPhase() == PomodoroTimerService.Phase.FOCUS) {
                updateGrowthFrame();
            }
        });

        // UI setup
        setupPlantList();
        if (!list.getStyleClass().contains("plant-list")) {
            list.getStyleClass().add("plant-list");
        }

        if (!list.getItems().isEmpty()) {
            list.getSelectionModel().selectFirst();
            setCurrentPlant(list.getSelectionModel().getSelectedItem());
        }

        setupRingInteraction();

        focusMode.setVisible(false);
        focusMode.setManaged(false);
        focusMode.selectedProperty().addListener((o, was, is) -> toggleFocus(is));

        btnStartReset.setOnAction(e -> onStartReset());
        if (btnConfig != null) {
            btnConfig.setOnAction(e -> {
                boolean ok = showTimerPopup();

                if (ok) {
                    updateTimerLabel(timerService.getTotalSeconds());
                    updateProgressBar();
                }
            });
        }

        // idle: mostra la thumbnail della pianta corrente
        showIdleImage();

        updateTimerLabel(timerService.getTotalSeconds());
        updateButtonUI(PomodoroTimerService.TimerState.IDLE);

        // check (ridondante) per assicurarsi che l'arco di completamento sia vuoto (trasparente)
        if (timerArc != null)
            timerArc.setFill(Color.TRANSPARENT);

        updateProgressBar();
    }

    // eventi servizio

    private void onPomodoroCompleted() {
        if (currentPlant != null) {
            // logica gamification
            gameState.onPomodoroCompleted(currentPlant);
            // log attività
            logPlantActivityForCurrentPomodoro();
        }
        showStage(3); // pianta completa
    }

    private void updateTimerLabel(int remaining) {
        int m = remaining / 60;
        int s = remaining % 60;
        lblTimer.setText(String.format("%02d:%02d", m, s));
    }

    private void updateProgressBar() {
        // aggiorne sia l'arco che il knob in base al tempo rimanente
        double total = timerService.getTotalSeconds();
        double remaining = timerService.getRemainingSeconds();

        // aggiornamento UI (se non in uso)
        if (!isDraggingInfo) {
            double ratio = (total <= 0) ? 0 : (remaining / total);

            // ratio va da 1.0 (inizio) a 0.0 (fine).
            // length da 0 (inizio) a -360 (fine).
            double progress = 1.0 - ratio;
            double angleLength = -360 * progress;

            timerArc.setLength(angleLength);
            updateKnobPosition(angleLength);
        }

        // barra sessione
        double sTotal = timerService.sessionTotalSecondsProperty().get();
        double sElapsed = timerService.sessionElapsedSecondsProperty().get();
        if (sessionProgress != null) {
            double frac = (sTotal <= 0) ? 0.0 : Math.min(1.0, sElapsed / sTotal);
            sessionProgress.setProgress(frac);
        }
    }

    private void updateButtonUI(PomodoroTimerService.TimerState state) {
        boolean running = (state == PomodoroTimerService.TimerState.RUNNING);
        btnStartReset.setText(running ? "Reset" : "Start");

        if (running) {
            btnStartReset.getStyleClass().remove("start");
            focusMode.setVisible(true);
            focusMode.setManaged(true);
        } else {
            if (!btnStartReset.getStyleClass().contains("start")) {
                btnStartReset.getStyleClass().add("start");
            }
            focusMode.setVisible(false);
            focusMode.setManaged(false);
            if (focusMode.isSelected()) {
                focusMode.setSelected(false); // toglie focus mode se attivo
            }
        }

        list.setDisable(running);
        list.setOpacity(running ? 0.6 : 1.0);

        enableRingInteraction(!running);

        if (btnConfig != null) {
            btnConfig.setDisable(running);
        }
    }

    // logica anello

    private void setupRingInteraction() {
        // gestione drag sul container o sul knob
        // si usa timerContainer per prendere anche click fuori dal knob ma sull'anello

        if (timerContainer != null) {
            timerContainer.setOnMousePressed(e -> handleMouseInteraction(e, true));
            timerContainer.setOnMouseDragged(e -> handleMouseInteraction(e, false));
            timerContainer.setOnMouseReleased(e -> isDraggingInfo = false);
        }
    }

    private void enableRingInteraction(boolean enable) {
        if (timerContainer != null) {
            timerContainer.setDisable(!enable);
            timerKnob.setVisible(enable);
        }
    }

    private void handleMouseInteraction(MouseEvent e, boolean isPressed) {
        if (timerService.getTimerState() != PomodoroTimerService.TimerState.IDLE)
            return;

        isDraggingInfo = true;

        // calcola l'angolo rispetto al centro del timerContainer
        double x = e.getX();
        double y = e.getY();
        double cx = timerContainer.getWidth() / 2;
        double cy = timerContainer.getHeight() / 2;

        // vettore dal centro
        double dx = x - cx;
        double dy = y - cy;

        // in javafx 0 = est, ma serve che 0 sia in alto
        double theta = Math.toDegrees(Math.atan2(dy, dx));

        double angle = theta + 90;
        if (angle < 0)
            angle += 360;

        double minutesRaw = (angle / 360.0) * MAX_MINUTES;
        int minutes = (int) Math.round(minutesRaw);
        if (minutes < 1)
            minutes = 1; // Minimo 1 minuto
        if (minutes > MAX_MINUTES)
            minutes = MAX_MINUTES;

        // imposta il timer service
        timerService.configureSingleTimer(minutes);

        updateTimerLabel(minutes * 60);

        // aggiorna anello
        double length = -angle; // Da 0 a -360
        timerArc.setLength(length);
        updateKnobPosition(length);
    }

    private void updateKnobPosition(double angleLength) {
        double thetaDeg = 90 + angleLength;
        double thetaRad = Math.toRadians(thetaDeg);

        double kx = RADIUS * Math.cos(thetaRad);
        double ky = RADIUS * Math.sin(thetaRad);

        timerKnob.setTranslateX(kx);

        timerKnob.setTranslateY(-ky);
    }

    // logica UI

    private void onStartReset() {
        if (timerService.getTimerState() == PomodoroTimerService.TimerState.IDLE) {

            if (timerService.getTotalSeconds() > 0) {
                timerService.start();
                showStage(0);
            } else {
                boolean ok = showTimerPopup();
                if (ok) {
                    timerService.start();
                    showStage(0);
                }
            }
        } else {
            timerService.reset(); //ferma e resetta stats

            if (timerService.getPhase() == PomodoroTimerService.Phase.FOCUS
                    && timerService.getRemainingSeconds() > 0
                    && currentPlant != null) {
                gameState.onPomodoroAborted(currentPlant);
                if (wiltFrame != null)
                    img.setImage(wiltFrame);
            } else {
                showStage(0);
            }

            // ripristina UI allo stato idle
            showIdleImage();

            updateTimerLabel(timerService.getTotalSeconds());
            updateProgressBar();
            enableRingInteraction(true);
        }
    }

    private boolean showTimerPopup() {
        try {
            URL fxml = getClass().getResource("/com/application/canopy/view/timer-dialog.fxml");
            if (fxml == null) {
                System.err.println("timer-dialog.fxml non trovato");
                //fallback: start diretto default se manca dialog
                return true;
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent content = loader.load();
            TimerDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Configura timer");
            DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);

            // passa riferimento del dialog al controller per chiuderlo
            controller.setDialog(dialog);

            // Stile
            URL css = getClass().getResource("/css/base.css");
            if (css != null)
                pane.getStylesheets().add(css.toExternalForm());
            ThemeManager.applyTheme(pane);

            pane.getButtonTypes().add(ButtonType.CLOSE);
            javafx.scene.Node closeButton = pane.lookupButton(ButtonType.CLOSE);
            closeButton.setVisible(false);
            closeButton.setManaged(false);

            var result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK)
                return false;

            // configura service in base alla scelta
            TimerDialogController.Choice choice = controller.getResult();
            if (choice == null)
                return false;

            if (choice.getType() == TimerDialogController.Choice.Type.PRESET) {
                var p = choice.getPreset();
                int macroCycles = Math.max(1, choice.getCycles());
                int focusPerMacro = Math.max(1, p.getRepeatBeforeLongBreak());
                // numero di timer focus totali = macro * focusPerMacro
                int totalFocusBlocks = macroCycles * focusPerMacro;

                timerService.configureSession(
                        p.getFocusMinutes(),
                        p.getShortBreakMinutes(),
                        p.getLongBreakMinutes(),
                        totalFocusBlocks,
                        true,
                        focusPerMacro);
            } else {
                timerService.configureSingleTimer(choice.getSingleMinutes());
            }

            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void logPlantActivityForCurrentPomodoro() {
        if (activityRepository == null || currentPlant == null)
            return;
        try {
            activityRepository.addActivity(
                    LocalDate.now(),
                    currentPlant.getName(),
                    timerService.getFocusMinutes());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Gestione piante

    private void setCurrentPlant(Plant p) {
        this.currentPlant = p;
        if (p != null)
            gameState.setCurrentPlantId(p.getId());
        if (timerService.getTimerState() == PomodoroTimerService.TimerState.IDLE) {
            loadImages(); //precarica immagini stage
            showIdleImage();
        } else {
            loadImages();
            updateGrowthFrame();
        }
    }

    private void setupPlantList() {
        // filtra solo le piante sbloccate
        var unlockedPlants = Plant.samplePlants().stream()
                .filter(p -> {
                    var state = gameState.getStateFor(p);
                    return state != null && state.isUnlocked();
                })
                .toList();

        list.getItems().setAll(unlockedPlants);
        list.setCellFactory(v -> new ListCell<>() {
            private final ImageView icon = new ImageView();
            private final Label title = new Label();
            private final Label subtitle = new Label();
            private final VBox texts = new VBox(2, title, subtitle);
            private final HBox root = new HBox(10, icon, texts);
            {
                setPrefWidth(0);
                icon.setFitWidth(48);
                icon.setFitHeight(48);
                icon.setPreserveRatio(true);
                title.setStyle("-fx-font-weight: bold");
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
                    //usa ResourceManager
                    icon.setImage(com.application.canopy.util.ResourceManager.getPlantThumbnail(p.getThumbFile()));
                    setGraphic(root);
                }
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> {
            if (p != null)
                setCurrentPlant(p);
        });
    }

    private void loadImages() {
        if (currentPlant == null)
            return;
        String folder = currentPlant.getFolderName();

        for (int i = 0; i < STAGE_FILES.length; i++) {
            frames[i] = com.application.canopy.util.ResourceManager.getStageImage(folder, i);
        }
        // il wilt frame per ora è stage0
        wiltFrame = com.application.canopy.util.ResourceManager.getStageImage(folder, 0);

        img.setVisible(true);
        img.setPreserveRatio(true);
    }

    private void updateGrowthFrame() {
        double total = timerService.getTotalSeconds();
        double remaining = timerService.getRemainingSeconds();
        double p = (total == 0) ? 0 : 1.0 - (remaining / total);

        int stage = (p >= 2.0 / 3.0) ? 2 : (p >= 1.0 / 3.0 ? 1 : 0);
        showStage(stage);
    }

    private void showStage(int idx) {
        if (idx >= 0 && idx < frames.length && frames[idx] != null) {
            img.setImage(frames[idx]);
        }
    }

    private void toggleFocus(boolean on) {
        com.application.canopy.Navigator.setFullScreen(on);
        if (rightPanel != null) {
            rightPanel.setVisible(!on);
            rightPanel.setManaged(!on);
        }
    }

    private void showIdleImage() {
        if (currentPlant != null) {
            //mostra icona/thumbnail
            img.setImage(com.application.canopy.util.ResourceManager.getPlantThumbnail(currentPlant.getThumbFile()));
        }
    }
}
