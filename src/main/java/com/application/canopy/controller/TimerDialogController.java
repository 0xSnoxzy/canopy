package com.application.canopy.controller;

import com.application.canopy.db.DatabaseManager;
import com.application.canopy.db.TimerDatabase;
import com.application.canopy.model.FontManager;
import com.application.canopy.model.ThemeManager;
import com.application.canopy.model.TimerPreset;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class TimerDialogController {

    @FXML
    private BorderPane root;
    @FXML
    private VBox presetContainer;
    @FXML
    private Button btnNewPreset;

    @FXML
    private TextField txtName;
    @FXML
    private TextField txtFocus;
    @FXML
    private TextField txtShort;
    @FXML
    private TextField txtLong;
    @FXML
    private Spinner<Integer> cycleSpinner;

    @FXML
    private Button btnCancel;
    @FXML
    private Button btnApply;

    // Azioni opzionali
    @FXML
    private Button btnSavePreset;
    @FXML
    private Button btnDeletePreset;
    @FXML
    private javafx.scene.layout.HBox presetActionsBox;
    @FXML
    private Label lblConfigTitle;

    private List<TimerPreset> presets = new ArrayList<>();
    private TimerPreset selectedPreset = null;

    private Dialog<ButtonType> dialog;

    public static class Choice {
        public enum Type {
            PRESET, SINGLE
        }

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

        public Type getType() {
            return type;
        }

        public TimerPreset getPreset() {
            return preset;
        }

        public int getCycles() {
            return cycles;
        }

        public int getSingleMinutes() {
            return singleMinutes;
        }
    }

    public void setDialog(Dialog<ButtonType> dialog) {
        this.dialog = dialog;
    }



    @FXML
    private void initialize() {
        if (root != null) {
            root.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    ThemeManager.applyTheme(root);
                    FontManager.setFont(FontManager.getCurrentFont(), newScene);
                }
            });
        }


        loadPresets();

        //valori di default se nulla è selezionato
        txtFocus.setText("25");
        txtShort.setText("5");
        txtLong.setText("15");

        btnNewPreset.setOnAction(e -> onNewPreset());
        btnApply.setOnAction(e -> onApply());
        btnCancel.setOnAction(e -> onCancel());

        if (btnSavePreset != null)
            btnSavePreset.setOnAction(e -> onSavePreset());
        if (btnDeletePreset != null)
            btnDeletePreset.setOnAction(e -> onDeletePreset());

        onNewPreset();
    }



    private void loadPresets() {
        presets.clear();
        try {
            Connection conn = DatabaseManager.getConnection();
            TimerDatabase.createTableIfNeeded(conn);
            TimerDatabase.ensureDefaults(conn);
            presets.addAll(TimerDatabase.findAll(conn));
        } catch (Exception e) {
            e.printStackTrace();
            // errore
            if (presets.isEmpty()) {
                presets.add(new TimerPreset("Pomodoro", 25, 5, 15, 4));
            }
        }
        renderPresetList();
    }

    private void renderPresetList() {
        presetContainer.getChildren().clear();
        for (TimerPreset p : presets) {
            Button btn = new Button(p.getName());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("button-list-item");
            // se la classe non esiste usa lo stile inline
            btn.setStyle("-fx-alignment: CENTER-LEFT;");

            btn.setOnAction(e -> selectPreset(p));
            presetContainer.getChildren().add(btn);
        }
    }

    private void selectPreset(TimerPreset p) {
        this.selectedPreset = p;
        if (txtName != null)
            txtName.setText(p.getName());
        txtFocus.setText(String.valueOf(p.getFocusMinutes()));
        txtShort.setText(String.valueOf(p.getShortBreakMinutes()));
        txtLong.setText(String.valueOf(p.getLongBreakMinutes()));

        if (cycleSpinner.getValueFactory() == null) {
            cycleSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, p.getRepeatBeforeLongBreak()));
        } else {
            cycleSpinner.getValueFactory().setValue(Math.max(1, p.getRepeatBeforeLongBreak()));
        }

        if (presetActionsBox != null) {
            presetActionsBox.setVisible(true);
            btnSavePreset.setText("Salva Modifiche");
            if (btnDeletePreset != null) {
                btnDeletePreset.setVisible(true);
                btnDeletePreset.setManaged(true);
            }
        }
        if (lblConfigTitle != null)
            lblConfigTitle.setText("Configura " + p.getName());
    }

    private void onNewPreset() {
        this.selectedPreset = null;
        if (txtName != null) {
            txtName.setText("");
            txtName.setPromptText("Nome Nuova Routine");
        }
        // reimposta ai valori pomodoro di default
        txtFocus.setText("25");
        txtShort.setText("5");
        txtLong.setText("15");
        if (cycleSpinner.getValueFactory() == null) {
            cycleSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 4));
        } else {
            cycleSpinner.getValueFactory().setValue(4);
        }

        if (presetActionsBox != null) {
            presetActionsBox.setVisible(true);
            btnSavePreset.setText("Salva Nuova Routine");
            if (btnDeletePreset != null) {
                btnDeletePreset.setVisible(false);
                btnDeletePreset.setManaged(false);
            }
        }
        if (lblConfigTitle != null)
            lblConfigTitle.setText("Nuova Sessione");
    }

    private void onSavePreset() {
        // parsing dei valori comuni ad entrambi i casi
        String name = (txtName != null && !txtName.getText().isBlank()) ? txtName.getText().trim()
                : "Routine Personalizzata";
        int focus = parseIntSafe(txtFocus.getText(), 25);
        int sBreak = parseIntSafe(txtShort.getText(), 5);
        int lBreak = parseIntSafe(txtLong.getText(), 15);
        int cycles = (cycleSpinner.getValue() != null) ? cycleSpinner.getValue() : 4;

        try {
            Connection conn = DatabaseManager.getConnection();

            if (selectedPreset != null) {
                //aggiorna
                selectedPreset.setName(name);
                selectedPreset.setFocusMinutes(focus);
                selectedPreset.setShortBreakMinutes(sBreak);
                selectedPreset.setLongBreakMinutes(lBreak);
                selectedPreset.setRepeatBeforeLongBreak(cycles);

                TimerDatabase.update(conn, selectedPreset);
            } else {
                // inserisci nuovo preset
                TimerPreset newPreset = new TimerPreset(name, focus, sBreak, lBreak, cycles);
                TimerDatabase.insert(conn, newPreset);
                // Cambia contesto al nuovo preset
                this.selectedPreset = newPreset;
            }

            // ricarica lista
            loadPresets();
            // riseleziona quello corrente per aggiornare lo stato della UI
            if (selectedPreset != null) {
                for (TimerPreset p : presets) {
                    if (p.getName().equals(selectedPreset.getName())
                            && p.getFocusMinutes() == selectedPreset.getFocusMinutes()) {
                        selectPreset(p);
                        break;
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onDeletePreset() {
        if (selectedPreset != null) {
            try {
                Connection conn = DatabaseManager.getConnection();
                TimerDatabase.delete(conn, selectedPreset);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            selectedPreset = null;
            loadPresets();
            onNewPreset();
        }
    }

    private void onApply() {
        if (dialog != null) {
            dialog.setResult(ButtonType.OK);
            dialog.close();
        }
    }

    private void onCancel() {
        if (dialog != null) {
            dialog.setResult(ButtonType.CANCEL);
            dialog.close();
        }
    }

    private int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    public Choice getResult() {
        // costruisci un Preset temporaneo basato sugli input correnti
        String name = (txtName != null && !txtName.getText().isBlank()) ? txtName.getText().trim() : "Sessione";
        int focus = parseIntSafe(txtFocus.getText(), 25);
        int sh = parseIntSafe(txtShort.getText(), 5);
        int lg = parseIntSafe(txtLong.getText(), 15);
        int cy = (cycleSpinner.getValue() != null) ? cycleSpinner.getValue() : 1;

        TimerPreset temp = new TimerPreset(name, focus, sh, lg, cy);
        return Choice.preset(temp, cy);
    }
}