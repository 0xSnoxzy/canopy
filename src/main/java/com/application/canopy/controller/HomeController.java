package com.application.canopy.controller;

import com.application.canopy.db.PlantActivityRepository;
import com.application.canopy.model.GameState;
import com.application.canopy.model.Plant;
import com.application.canopy.model.ThemeManager;
import com.application.canopy.service.PomodoroTimerService;
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
    private Canvas canvas;
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
    private ListView<Plant> list;

    // servizi/modello
    private final GameState gameState = GameState.getInstance();
    private final PomodoroTimerService timerService = new PomodoroTimerService();
    private PlantActivityRepository activityRepository;

    private Plant currentPlant;

    // ----------------- IMMAGINI PIANTE (provvisorio) -----------------

    private static final String[] STAGE_FILES = { "stage0.png", "stage1.png", "stage2.png", "stage3.png" };

    private final Image[] frames = new Image[4];
    private Image wiltFrame = null;

    @FXML
    private void initialize() {

        // ✅ Inizializza repository via Locator
        activityRepository = com.application.canopy.service.ServiceLocator.getInstance().getPlantActivityRepository();

        // Configurazione iniziale di default (25 min single)
        timerService.configureSingleTimer(25);

        // Listener eventi timer
        timerService.setOnPomodoroCompleted(this::onPomodoroCompleted);

        // Binding UI agli stati del Service
        timerService.remainingSecondsProperty().addListener((o, old, val) -> updateTimerLabel(val.intValue()));
        timerService.timerStateProperty().addListener((o, old, state) -> updateButtonUI(state));

        // Listener per progresso barra
        timerService.remainingSecondsProperty().addListener((o, old, val) -> {
            updateProgressBar();
            // Aggiorna anche la pianta se siamo in running
            if (timerService.getTimerState() == PomodoroTimerService.TimerState.RUNNING &&
                    timerService.getPhase() == PomodoroTimerService.Phase.FOCUS) {
                updateGrowthFrame();
            }
        });

        // UI Setup
        if (canvas != null) {
            canvas.setVisible(false);
            canvas.setManaged(false);
        }

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
                // Se ok (configurato), aggiorna label/bar?
                // showTimerPopup chiama già timerService.configure...
                if (ok) {
                    updateTimerLabel(timerService.getTotalSeconds());
                    updateProgressBar();
                }
            });
        }

        // Inizializza UI statica
        // Idle: mostra thumbnail pianta corrente
        showIdleImage();

        updateTimerLabel(timerService.getTotalSeconds());
        updateButtonUI(PomodoroTimerService.TimerState.IDLE);

        // Ensure Arc fill is transparent (redundant check for robust visuals)
        if (timerArc != null)
            timerArc.setFill(Color.TRANSPARENT);

        // Initial visual update
        updateProgressBar();
    }

    //

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
        // Aggiorna la l'arco e il knob in base al tempo rimanente
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
        // Gestione drag sul container o sul knob
        // Per semplicità intercettiamo sul parent (timerContainer) o sul knob.
        // Intercettiamo su timerContainer per prendere anche click "fuori" dal knob ma
        // sull'anello.

        if (timerContainer != null) {
            timerContainer.setOnMousePressed(e -> handleMouseInteraction(e, true));
            timerContainer.setOnMouseDragged(e -> handleMouseInteraction(e, false));
            timerContainer.setOnMouseReleased(e -> isDraggingInfo = false);
        }
    }

    private void enableRingInteraction(boolean enable) {
        if (timerContainer != null) {
            timerContainer.setDisable(!enable);
            // Opzionale: cambiare cursore o opacità
            timerKnob.setVisible(enable); // Nascondi knob se running? O lascialo fisso. Forest lo nasconde o lo blocca.
            // Lasciamolo visibile ma non interattivo (via setDisable)
        }
    }

    private void handleMouseInteraction(MouseEvent e, boolean isPressed) {
        if (timerService.getTimerState() != PomodoroTimerService.TimerState.IDLE)
            return;

        isDraggingInfo = true;

        // Calcola angolo rispetto al centro del timerContainer
        // timerContainer è 720x720 (max), ma il cerchio è al centro.
        // Coordinate locali evento
        double x = e.getX();
        double y = e.getY();
        double cx = timerContainer.getWidth() / 2;
        double cy = timerContainer.getHeight() / 2;

        // Vettore dal centro
        double dx = x - cx;
        double dy = y - cy;

        // Angolo in gradi. Atan2(y, x).
        // JavaFX: 0 è est (3 o'clock), positivo orario.
        // Noi vogliamo 0 a Nord (12 o'clock).
        double theta = Math.toDegrees(Math.atan2(dy, dx));

        // Converti in sistema "Nord = 0, Clockwise"
        // Atan2: Est=0, Sud=90, Ovest=180/-180, Nord=-90
        // Ruotiamo di +90
        double angle = theta + 90;
        if (angle < 0)
            angle += 360;

        // angle va da 0 (Nord) a 360 (Nord) in senso orario.

        // Snap ai minuti? (es. step 5 min o 1 min)
        // Mappatura: 0..360 gradi -> 0..MAX_MINUTES
        double minutesRaw = (angle / 360.0) * MAX_MINUTES;
        int minutes = (int) Math.round(minutesRaw);
        if (minutes < 1)
            minutes = 1; // Minimo 1 minuto
        if (minutes > MAX_MINUTES)
            minutes = MAX_MINUTES;

        // Imposta il timer service (Configura SOLO quando siamo idle)
        timerService.configureSingleTimer(minutes);

        // Aggiorna UI immediata
        updateTimerLabel(minutes * 60);

        // Aggiorna anello (length negativo per clockwise da 90deg start)
        double length = -angle; // Da 0 a -360
        timerArc.setLength(length);
        updateKnobPosition(length);
    }

    private void updateKnobPosition(double angleLength) {
        // angleLength è negativo (es. -90 per 3 o'clock partendo da 12)
        // StartAngle arc è 90 (Nord).
        // Current angle degrees = 90 + angleLength.
        double thetaDeg = 90 + angleLength;
        double thetaRad = Math.toRadians(thetaDeg);

        // Posizione su cerchio (cx=0, cy=0 relative to stackpane center if translated,
        // ma in StackPane i figli sono centrati. TranslateX/Y sposta dal centro).
        double kx = RADIUS * Math.cos(thetaRad);
        double ky = RADIUS * Math.sin(thetaRad); // Y axis giù è positivo in JavaFX

        // NOTA: JavaFX Y-axis: Giù positivo.
        // Math coords: solito (Y su positivo).
        // Ma qui stiamo ruotando, cos/sin standard su cerchio trigonometrico vanno bene
        // se consideriamo gli assi screen.
        // 0 deg (Est) -> cos=1, sin=0 -> X=R, Y=0. Corretto.
        // 90 deg (Sud) -> cos=0, sin=1 -> X=0, Y=R. Corretto (scende).
        // -90 deg (Nord) -> cos=0, sin=-1 -> X=0, Y=-R. Corretto (sale).
        // Quindi thetaDeg calcolato sopra deve essere corretto rispetto a queste
        // coordinate.
        // StartAngle 90 (Arc) in JavaFX Shape significa NORD?
        // Arc: "startAngle - Starting angle in degrees measured CA from the positive
        // x-axis".
        // Quindi 0 è Est. 90 è Nord (Counter-Clockwise per Arc/Shape positive
        // interaction? No, standard math).
        // Aspetta, Arc documentation: "measured counter-clockwise from the positive
        // x-axis".
        // Quindi 90 è Nord. 180 Ovest. 270 Sud.
        // Se setLength è NEGATIVO, va in Clockwise (come vogliamo noi).
        // Quindi se length = -90. Angle = 90 + (-90) = 0 (Est).

        // Quello che ho calcolato in updateKnobPosition:
        // thetaDeg = 90 + angleLength. Se length -90 -> 0 deg (Est).
        // Ma Math.cos(0) = 1, Math.sin(0) = 0. -> (R, 0). Est.
        // Ma Arc a Est ha disegnato?
        // Start 90 (Nord). Length -90 (Clockwise fino a Est).
        // L'arco copre da Nord a Est. Il knob deve essere a Est.
        // Quindi sembra corretto.

        // Però nel handlerMouse, ho calcolato l'angolo in senso ORARIO dal Nord.
        // angle (0..360).
        // length = -angle.
        // Quindi se muovo il mouse a Est (90 deg orari da Nord).
        // angle = 90. length = -90.
        // thetaDeg = 0. (Est). Knob a Est.
        // Sembra coerente.

        timerKnob.setTranslateX(kx);
        // Poiché Y screen è invertita rispetto a piano cartesiano standard?
        // No, in Screen coords Y cresce in giù.
        // Math.sin(theta): per 90 deg (Sud visualmente?) -> 1. -> Y=R. Giù.
        // Ma Arc startAngle 90 è NORD (Y negativa su screen?).
        // Verifica: In JavaFX Arc, positive angles are Counter Clockwise from X axis.
        // 0 = Est. 90 = Nord (Up, Y-).
        // Quindi Math.sin(90 deg) = 1. Ma noi vogliamo Y = -R.
        // Dobbiamo invertire il sin? O usare -angle?

        // Controllo semplice:
        // thetaDeg = 90 (Nord). cos=0. sin=1. -> Y=R (Giù). SBAGLIATO.
        // Vogliamo Y=-R (Su).
        // Quindi ky = -RADIUS * Math.sin(thetaRad);

        timerKnob.setTranslateY(-ky);
    }

    // ----------------- LOGICA UI -----------------

    private void onStartReset() {
        if (timerService.getTimerState() == PomodoroTimerService.TimerState.IDLE) {
            // Se l'utente ha selezionato un tempo tramite ghiera, usiamo quello?
            // O mostriamo comunque il popup?
            // Forest logic: la ghiera imposta il tempo, poi clicchi "Pianta" (Start).
            // Se l'utente ha trascinato la ghiera, probabilmente vuole avviare quel tempo.
            // Se è a 0 o default, magari mostriamo popup?
            // Per semplicità: se il timer è configurato (duration > 0), start direct.
            // Altrimenti (o se vogliamo preset avanzati), popup.

            // Controlliamo se la durata corrente è valida (>0)
            if (timerService.getTotalSeconds() > 0) {
                // START
                timerService.start();
                // Passa a visualizzazione stage (0 o in base a progresso)
                showStage(0);
            } else {
                // Fallback o popup se 0
                boolean ok = showTimerPopup();
                if (ok) {
                    timerService.start();
                    showStage(0);
                }
            }
        } else {
            // RESET
            timerService.reset(); // Ferma e resetta stats

            // Abort logic (gamification)
            if (timerService.getPhase() == PomodoroTimerService.Phase.FOCUS
                    && timerService.getRemainingSeconds() > 0
                    && currentPlant != null) {
                gameState.onPomodoroAborted(currentPlant);
                if (wiltFrame != null)
                    img.setImage(wiltFrame);
            } else {
                showStage(0);
            }

            // Ripristina UI allo stato idle
            showIdleImage();

            updateTimerLabel(timerService.getTotalSeconds());
            // Reset Arc to full? Or to set time?
            // Usually reset to the set time.
            updateProgressBar();
            // Rabilita interaction
            enableRingInteraction(true);
        }
    }

    private boolean showTimerPopup() {
        try {
            URL fxml = getClass().getResource("/com/application/canopy/view/timer-dialog.fxml");
            if (fxml == null) {
                System.err.println("timer-dialog.fxml non trovato");
                // Fallback: start diretto default se manca dialog (per debug)
                return true;
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent content = loader.load();
            TimerDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Configura timer");
            DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);

            // Fix for macOS full screen: set owner to main window
            if (root.getScene() != null && root.getScene().getWindow() != null) {
                dialog.initOwner(root.getScene().getWindow());
            }

            // Pass direct dialog reference to controller so it can close it
            controller.setDialog(dialog);

            // Stile
            URL css = getClass().getResource("/css/base.css");
            if (css != null)
                pane.getStylesheets().add(css.toExternalForm());
            ThemeManager.applyTheme(pane);

            // Hide default buttons (we use custom buttons in FXML)
            // But we need close/X button? DialogPane handles strict content.
            // If we don't add button types, it's just the content.
            pane.getButtonTypes().add(ButtonType.CLOSE);
            javafx.scene.Node closeButton = pane.lookupButton(ButtonType.CLOSE);
            closeButton.setVisible(false);
            closeButton.setManaged(false);

            var result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK)
                return false;

            // Configura Service in base alla scelta
            TimerDialogController.Choice choice = controller.getResult();
            if (choice == null)
                return false;

            if (choice.getType() == TimerDialogController.Choice.Type.PRESET) {
                var p = choice.getPreset();
                int macroCycles = Math.max(1, choice.getCycles());
                int focusPerMacro = Math.max(1, p.getRepeatBeforeLongBreak());
                // Totale focus "singoli" = macro * focusPerMacro
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

    // ----------------- GESTIONE PIANTE / IMMAGINI -----------------

    private void setCurrentPlant(Plant p) {
        this.currentPlant = p;
        if (p != null)
            gameState.setCurrentPlantId(p.getId());
        if (timerService.getTimerState() == PomodoroTimerService.TimerState.IDLE) {
            loadImages(); // Precarica immagini stage
            showIdleImage();
        } else {
            // Se cambia mentre corre (disabilitato da UI, ma possibile via codice),
            // aggiorna
            loadImages();
            updateGrowthFrame();
        }
    }

    private void setupPlantList() {
        // Filtra solo le piante sbloccate
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
                    // Usa ResourceManager
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
        // Il wilt frame è sempre stage0 per ora nel codice originale
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
    }

    private void showIdleImage() {
        if (currentPlant != null) {
            // Mostra icona/thumbnail
            img.setImage(com.application.canopy.util.ResourceManager.getPlantThumbnail(currentPlant.getThumbFile()));
        }
    }
}
