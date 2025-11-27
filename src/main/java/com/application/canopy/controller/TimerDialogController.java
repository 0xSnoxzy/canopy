package com.application.canopy.controller;

import com.application.canopy.model.FontManager;
import com.application.canopy.model.ThemeManager;

import com.application.canopy.db.DatabaseManager;
import com.application.canopy.db.TimerPresetDao;
import com.application.canopy.model.TimerPreset;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class TimerDialogController {

    // --- FXML -------------------------------------------------------
    @FXML private BorderPane root;

    @FXML private VBox leftSection;
    @FXML private VBox presetContainer;

    @FXML private StackPane rightSection;
    @FXML private StackPane wheelContainer;
    @FXML private VBox editorContainer;
    @FXML private VBox newPresetContainer;

    @FXML private Button btnEditPreset;
    @FXML private Button btnAddPreset;
    @FXML private Spinner<Integer> cycleSpinner;

    @FXML private TextField txtName;
    @FXML private TextField txtFocus;
    @FXML private TextField txtRepeat;
    @FXML private TextField txtShort;
    @FXML private TextField txtLong;
    @FXML private Button btnSavePreset;
    @FXML private Button btnDeletePreset;
    @FXML private Button btnCancelEdit;

    @FXML private TextField txtNewName;
    @FXML private TextField txtNewFocus;
    @FXML private TextField txtNewRepeat;
    @FXML private TextField txtNewShort;
    @FXML private TextField txtNewLong;
    @FXML private Button btnAddNewPreset;
    @FXML private Button btnCancelNew;

    // --- Stato logico -----------------------------------------------

    private enum Mode { PRESET, WHEEL }
    private Mode activeMode = null;          // ultima sezione “scelta”
    private boolean inPresetEditor = false;  // true se sto modificando/creando una routine

    private final List<TimerPreset> presets = new ArrayList<>();
    private TimerPreset selectedPreset;
    private Button selectedPresetButton;

    // ruota singolo timer
    private Canvas wheelCanvas;
    private TextField wheelField;   // numero al centro, scrivibile

    private static final int MIN_MINUTES = 0;
    private static final int MAX_MINUTES = 120;

    private int singleTimerMinutes = 0;
    private double knobAngle = 0; // 0° in alto

    // --- Risultato per HomeController -------------------------------

    public static class Choice {
        public enum Type { PRESET, SINGLE }

        private final Type type;
        private final TimerPreset preset;
        private final int cycles;
        private final int singleMinutes;

        private Choice(Type t, TimerPreset p, int cycles, int singleMinutes) {
            this.type = t;
            this.preset = p;
            this.cycles = cycles;
            this.singleMinutes = singleMinutes;
        }

        public static Choice preset(TimerPreset p, int cycles) {
            return new Choice(Type.PRESET, p, cycles, 0);
        }
        public static Choice single(int minutes) {
            return new Choice(Type.SINGLE, null, 0, minutes);
        }

        public Type getType() { return type; }
        public TimerPreset getPreset() { return preset; }
        public int getCycles() { return cycles; }
        public int getSingleMinutes() { return singleMinutes; }
    }


    @FXML
    private void initialize() {
        // Quando il dialog ottiene una Scene, applica tema + font correnti
        if (root != null) {
            root.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    // tema (classi .root.theme-*)
                    ThemeManager.applyTheme(root);
                    // font (classi .root.font-*)
                    FontManager.setFont(FontManager.getCurrentFont(), newScene);
                }
            });
        }

        loadPresets();
        setupPresetButtons();
        setupWheel();
        setupCyclesSpinner();
        setupSectionInteractions();
        setupEditor();

        // opacità iniziale: entrambe “oscurate”
        leftSection.setOpacity(0.35);
        rightSection.setOpacity(0.35);

        if (selectedPreset != null) {
            setActiveMode(Mode.PRESET);
        }
    }


    // --- Caricamento / lista routine --------------------------------

    private void loadPresets() {
        presets.clear();

        try {
            Connection conn = DatabaseManager.getConnection();
            TimerPresetDao.createTableIfNeeded(conn);
            TimerPresetDao.ensureDefaults(conn);

            presets.addAll(TimerPresetDao.findAll(conn));

        } catch (Exception e) {
            e.printStackTrace();

            if (presets.isEmpty()) {
                presets.add(new TimerPreset("Pomodoro", 25, 5, 15, 3));
                presets.add(new TimerPreset("Concentrazione profonda", 60, 10, 20, 2));
            }
        }
    }

    private void setupPresetButtons() {
        presetContainer.getChildren().clear();
        selectedPresetButton = null;

        for (TimerPreset p : presets) {
            addPresetButton(p);
        }

        if (!presets.isEmpty()) {
            if (selectedPreset == null) {
                selectedPreset = presets.getFirst();
            }
            // trova il bottone associato al preset selezionato
            for (int i = 0; i < presets.size(); i++) {
                if (presets.get(i) == selectedPreset &&
                        i < presetContainer.getChildren().size() &&
                        presetContainer.getChildren().get(i) instanceof Button b) {

                    selectedPresetButton = b;
                    break;
                }
            }
            highlightSelectedPreset();
            loadPresetIntoEditor(selectedPreset);
        }
    }

    private void addPresetButton(TimerPreset p) {
        Button btn = new Button(p.getDisplayText());
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinHeight(40);
        btn.setStyle("-fx-background-color: transparent;");

        btn.setOnAction(e -> {
            selectedPreset = p;
            selectedPresetButton = btn;
            highlightSelectedPreset();
            loadPresetIntoEditor(p);
            setActiveMode(Mode.PRESET);
        });

        presetContainer.getChildren().add(btn);
    }

    private void highlightSelectedPreset() {
        for (var node : presetContainer.getChildren()) {
            if (node instanceof Button b) {
                if (b == selectedPresetButton) {
                    b.setOpacity(1.0);
                    b.setStyle("-fx-background-color: rgba(255,255,255,0.12);");
                } else {
                    b.setOpacity(0.6);
                    b.setStyle("-fx-background-color: transparent;");
                }
            }
        }
    }

    // --- Ruota singolo timer ---------------------------------------

    private void setupWheel() {
        wheelCanvas = new Canvas(300, 300);

        wheelField = new TextField();
        wheelField.setText(String.valueOf(singleTimerMinutes));
        wheelField.setAlignment(Pos.CENTER);
        wheelField.setMaxWidth(100);
        wheelField.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: -canopy-text;"
        );

        // sync numero → ruota
        wheelField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                return;
            }
            try {
                int m = Integer.parseInt(newVal.trim());
                m = clamp(m, MIN_MINUTES, MAX_MINUTES);
                singleTimerMinutes = m;

                double fraction = singleTimerMinutes / (double) MAX_MINUTES;
                knobAngle = fraction * 360.0; // 0–360, 0 = alto

                drawWheel();
            } catch (NumberFormatException ignored) {
                // input non valido: ignoriamo, non modifichiamo il valore
            }
        });

        StackPane.setAlignment(wheelCanvas, Pos.CENTER);
        StackPane.setAlignment(wheelField, Pos.CENTER);

        wheelContainer.getChildren().clear();
        wheelContainer.getChildren().addAll(wheelCanvas, wheelField);

        drawWheel();

        // ruota → numero
        wheelCanvas.setOnMouseDragged(e -> {
            updateAngleFromMouse(e.getX(), e.getY());
            if (!inPresetEditor) {
                setActiveMode(Mode.WHEEL);
            }
            wheelField.setText(String.valueOf(singleTimerMinutes));
        });

        wheelCanvas.setOnMousePressed(e -> {
            updateAngleFromMouse(e.getX(), e.getY());
            if (!inPresetEditor) {
                setActiveMode(Mode.WHEEL);
            }
            wheelField.setText(String.valueOf(singleTimerMinutes));
        });
    }

    private void updateAngleFromMouse(double mouseX, double mouseY) {
        double cx = wheelCanvas.getWidth() / 2.0;
        double cy = wheelCanvas.getHeight() / 2.0;

        double dx = mouseX - cx;
        double dy = mouseY - cy;

        double rawAngle = Math.toDegrees(Math.atan2(dy, dx));
        // 0° in alto, senso orario
        double angle = (rawAngle + 450) % 360;

        knobAngle = angle;

        double fraction = angle / 360.0;
        int minutes = (int) Math.round(MIN_MINUTES + fraction * (MAX_MINUTES - MIN_MINUTES));
        singleTimerMinutes = clamp(minutes, MIN_MINUTES, MAX_MINUTES);

        drawWheel();
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void drawWheel() {
        GraphicsContext g = wheelCanvas.getGraphicsContext2D();
        double w = wheelCanvas.getWidth();
        double h = wheelCanvas.getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;

        double ringWidth = 20; // spessore dell’anello
        double radiusOuter = Math.min(w, h) / 2.0 - 10;
        double radiusInner = radiusOuter - ringWidth;

        g.clearRect(0, 0, w, h);

        // ---------- track base ----------
        g.setStroke(Color.LIGHTGRAY);
        g.setLineWidth(ringWidth);
        g.strokeOval(cx - radiusOuter, cy - radiusOuter,
                radiusOuter * 2, radiusOuter * 2);

        // ---------- arco di progresso (ancorato in alto, orario) ----------
        double fraction = singleTimerMinutes / (double) MAX_MINUTES;
        double extent = 360 * fraction;

        g.setStroke(Color.DARKGRAY);
        g.setLineWidth(ringWidth);
        g.strokeArc(cx - radiusOuter, cy - radiusOuter,
                radiusOuter * 2, radiusOuter * 2,
                90, -extent, ArcType.OPEN);

        // ---------- cerchio interno ----------
        g.setStroke(Color.GRAY);
        g.setLineWidth(2);
        g.strokeOval(cx - radiusInner, cy - radiusInner,
                radiusInner * 2, radiusInner * 2);

        // ---------- tacche ogni 15 minuti ----------
        g.setStroke(Color.GRAY);
        g.setLineWidth(2);

        int minutesStep = 15;
        int numTicks = MAX_MINUTES / minutesStep; // 120/15 = 8

        double tickInnerR = radiusOuter + 2;
        double tickOuterR = tickInnerR + 10;

        for (int i = 0; i < numTicks; i++) {
            double angleDeg = -90 + (360.0 / numTicks) * i; // partiamo dall’alto
            double rad = Math.toRadians(angleDeg);

            double x1 = cx + Math.cos(rad) * tickInnerR;
            double y1 = cy + Math.sin(rad) * tickInnerR;
            double x2 = cx + Math.cos(rad) * tickOuterR;
            double y2 = cy + Math.sin(rad) * tickOuterR;

            g.strokeLine(x1, y1, x2, y2);
        }

        // ---------- maniglia ----------
        double knobRadius = 12;
        double knobRad = Math.toRadians(knobAngle - 90); // 0° = alto
        double knobX = cx + Math.cos(knobRad) * radiusOuter;
        double knobY = cy + Math.sin(knobRad) * radiusOuter;

        g.setFill(Color.DIMGRAY);
        g.fillOval(knobX - knobRadius, knobY - knobRadius,
                knobRadius * 2, knobRadius * 2);

        // ---------- sincronizza il numero al centro ----------
        if (wheelField != null) {
            String target = String.valueOf(singleTimerMinutes);
            if (!target.equals(wheelField.getText())) {
                wheelField.setText(target);
            }
        }
    }

    // --- Cicli pomodoro ---------------------------------------------

    private void setupCyclesSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory vf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 1);
        cycleSpinner.setValueFactory(vf);
    }

    // --- Hover & click sulle due sezioni ----------------------------

    private void setupSectionInteractions() {
        // sezione sinistra (routine + cicli)
        leftSection.setOnMouseEntered(e -> {
            if (inPresetEditor) return;
            if (activeMode != Mode.PRESET) leftSection.setOpacity(0.6);
        });
        leftSection.setOnMouseExited(e -> {
            if (inPresetEditor) return;
            if (activeMode != Mode.PRESET) leftSection.setOpacity(0.35);
        });
        leftSection.setOnMouseClicked(e -> {
            if (!inPresetEditor) setActiveMode(Mode.PRESET);
        });

        // sezione destra (ruota / editor / nuovo)
        rightSection.setOnMouseEntered(e -> {
            if (inPresetEditor) return;
            if (activeMode != Mode.WHEEL) rightSection.setOpacity(0.6);
        });
        rightSection.setOnMouseExited(e -> {
            if (inPresetEditor) return;
            if (activeMode != Mode.WHEEL) rightSection.setOpacity(0.35);
        });
        rightSection.setOnMouseClicked(e -> {
            if (!inPresetEditor) setActiveMode(Mode.WHEEL);
        });

        btnEditPreset.setOnAction(e -> showEditPreset());
        btnAddPreset.setOnAction(e -> showNewPreset());
    }

    private void setActiveMode(Mode mode) {
        this.activeMode = mode;

        if (inPresetEditor) {
            rightSection.setOpacity(1.0);
            leftSection.setOpacity(0.35);
            return;
        }

        if (mode == Mode.PRESET) {
            leftSection.setOpacity(1.0);
            rightSection.setOpacity(0.35);
        } else {
            rightSection.setOpacity(1.0);
            leftSection.setOpacity(0.35);
        }
    }

    // --- Editor routine (Modifica / Nuovo) ---------------------------

    private void setupEditor() {
        editorContainer.setVisible(false);
        editorContainer.setManaged(false);

        newPresetContainer.setVisible(false);
        newPresetContainer.setManaged(false);

        btnSavePreset.setOnAction(e -> onSavePreset());
        btnDeletePreset.setOnAction(e -> onDeletePreset());
        btnAddNewPreset.setOnAction(e -> onAddNewPreset());

        btnCancelEdit.setOnAction(e -> closeEditorPanels());
        btnCancelNew.setOnAction(e -> closeEditorPanels());
    }

    private void showEditPreset() {
        if (selectedPreset == null && !presets.isEmpty()) {
            selectedPreset = presets.getFirst();
        }
        loadPresetIntoEditor(selectedPreset);

        inPresetEditor = true;

        editorContainer.setVisible(true);
        editorContainer.setManaged(true);

        newPresetContainer.setVisible(false);
        newPresetContainer.setManaged(false);

        wheelContainer.setVisible(false);
        wheelContainer.setManaged(false);

        rightSection.setOpacity(1.0);
        leftSection.setOpacity(0.35);
        activeMode = Mode.PRESET;
    }

    private void showNewPreset() {
        txtNewName.clear();
        txtNewFocus.clear();
        txtNewRepeat.clear();
        txtNewShort.clear();
        txtNewLong.clear();

        inPresetEditor = true;

        newPresetContainer.setVisible(true);
        newPresetContainer.setManaged(true);

        editorContainer.setVisible(false);
        editorContainer.setManaged(false);

        wheelContainer.setVisible(false);
        wheelContainer.setManaged(false);

        rightSection.setOpacity(1.0);
        leftSection.setOpacity(0.35);
        activeMode = Mode.PRESET;
    }

    private void closeEditorPanels() {
        inPresetEditor = false;

        editorContainer.setVisible(false);
        editorContainer.setManaged(false);

        newPresetContainer.setVisible(false);
        newPresetContainer.setManaged(false);

        wheelContainer.setVisible(true);
        wheelContainer.setManaged(true);

        if (activeMode == Mode.PRESET) {
            leftSection.setOpacity(1.0);
            rightSection.setOpacity(0.35);
        } else if (activeMode == Mode.WHEEL) {
            rightSection.setOpacity(1.0);
            leftSection.setOpacity(0.35);
        } else {
            leftSection.setOpacity(0.35);
            rightSection.setOpacity(0.35);
        }
    }

    private void loadPresetIntoEditor(TimerPreset p) {
        if (p == null) return;
        txtName.setText(p.getName());
        txtFocus.setText(String.valueOf(p.getFocusMinutes()));
        txtRepeat.setText(String.valueOf(p.getRepeatBeforeLongBreak()));
        txtShort.setText(String.valueOf(p.getShortBreakMinutes()));
        txtLong.setText(String.valueOf(p.getLongBreakMinutes()));
    }

    private void onSavePreset() {
        if (selectedPreset == null) return;

        String name = txtName.getText().trim();
        int focus  = parseIntSafe(txtFocus.getText(), selectedPreset.getFocusMinutes());
        int repeat = parseIntSafe(txtRepeat.getText(), selectedPreset.getRepeatBeforeLongBreak());
        int sBreak = parseIntSafe(txtShort.getText(), selectedPreset.getShortBreakMinutes());
        int lBreak = parseIntSafe(txtLong.getText(), selectedPreset.getLongBreakMinutes());

        if (!name.isEmpty()) selectedPreset.setName(name);
        selectedPreset.setFocusMinutes(Math.max(1, focus));
        selectedPreset.setRepeatBeforeLongBreak(Math.max(1, repeat));
        selectedPreset.setShortBreakMinutes(Math.max(0, sBreak));
        selectedPreset.setLongBreakMinutes(Math.max(0, lBreak));

        try {
            Connection conn = DatabaseManager.getConnection();
            if (selectedPreset.getId() > 0) {
                TimerPresetDao.update(conn, selectedPreset);
            } else {
                TimerPresetDao.insert(conn, selectedPreset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadPresets();
        setupPresetButtons();
        closeEditorPanels();
    }

    private void onDeletePreset() {
        if (selectedPreset == null) return;
        if (presets.size() <= 1) {
            return; // non lasciare l’utente senza almeno una routine
        }

        try {
            Connection conn = DatabaseManager.getConnection();
            TimerPresetDao.delete(conn, selectedPreset);
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadPresets();
        setupPresetButtons();
        closeEditorPanels();
    }

    private void onAddNewPreset() {
        String name = txtNewName.getText().trim();
        if (name.isEmpty()) return;

        int focus  = parseIntSafe(txtNewFocus.getText(), 25);
        int repeat = parseIntSafe(txtNewRepeat.getText(), 3);
        int sBreak = parseIntSafe(txtNewShort.getText(), 5);
        int lBreak = parseIntSafe(txtNewLong.getText(), 15);

        TimerPreset newPreset = new TimerPreset(
                name,
                Math.max(1, focus),
                Math.max(0, sBreak),
                Math.max(0, lBreak),
                Math.max(1, repeat)
        );

        try {
            Connection conn = DatabaseManager.getConnection();
            TimerPresetDao.insert(conn, newPreset);
        } catch (Exception e) {
            e.printStackTrace();
        }

        presets.add(newPreset);

        txtNewName.clear();
        txtNewFocus.clear();
        txtNewRepeat.clear();
        txtNewShort.clear();
        txtNewLong.clear();

        selectedPreset = newPreset;
        setupPresetButtons();
        closeEditorPanels();
        setActiveMode(Mode.PRESET);
    }

    private int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // --- Risultato per HomeController --------------------------------

    public Choice getResult() {
        if (activeMode == Mode.PRESET && selectedPreset != null) {
            int cycles = (cycleSpinner.getValue() == null ? 1 : cycleSpinner.getValue());
            return Choice.preset(selectedPreset, cycles);
        }
        int minutes = Math.max(1, singleTimerMinutes);
        return Choice.single(minutes);
    }
}
