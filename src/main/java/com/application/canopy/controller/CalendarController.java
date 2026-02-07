package com.application.canopy.controller;

import com.application.canopy.db.PlantActivityRepository;
import com.application.canopy.model.PlantActivity;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarController {

    @FXML
    private BorderPane root;

    @FXML
    private ToggleButton monthBtn, weekBtn;
    @FXML
    private Button prevBtn, nextBtn;
    @FXML
    private Label periodLabel, rightTitle, summaryLabel;
    @FXML
    private GridPane calendarGrid, weekdayHeader;
    @FXML
    private ListView<PlantStat> listMonth;

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
    private final Locale locale = Locale.ITALY;

    // Repository da cui prendo i dati delle piante
    private PlantActivityRepository repository;

    // Giorno -> Lista minuti per pianta del giorno
    private final Map<LocalDate, List<PlantStat>> dailyStats = new HashMap<>();

    @FXML
    private void initialize() {
        // Inizializzazione repository da PlantActivityRepository che prende dal DB
        repository = com.application.canopy.service.ServiceLocator.getInstance().getPlantActivityRepository();

        buildWeekdayHeader();

        monthBtn.setOnAction(e -> {
            weekBtn.setSelected(false);
            refresh();
        });
        weekBtn.setOnAction(e -> {
            monthBtn.setSelected(false);
            refresh();
        });

        prevBtn.setOnAction(e -> {
            if (monthBtn.isSelected())
                currentMonth = currentMonth.minusMonths(1);
            else
                currentWeekStart = currentWeekStart.minusWeeks(1);
            refresh();
        });
        nextBtn.setOnAction(e -> {
            if (monthBtn.isSelected())
                currentMonth = currentMonth.plusMonths(1);
            else
                currentWeekStart = currentWeekStart.plusWeeks(1);
            refresh();
        });

        listMonth.setCellFactory(v -> new PlantCell());

        // Aggiornamento per caricare eventuali cambi nel DB al primo caricamento
        refresh();

        // Listener che aggiorna il calendario appena viene caricato, se il newParent è
        // il calendar allora aggiorna dal DB per visualizzare
        // eventuali cambi in background mentre si era in altre pagine
        root.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                refresh();
            }
        });
    }

    // Costruzione della griglia degli header (Lun, Mar, Mer...)
    private void buildWeekdayHeader() {
        weekdayHeader.getChildren().clear();
        weekdayHeader.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            weekdayHeader.getColumnConstraints().add(cc);
        }
        DayOfWeek[] order = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };
        for (int c = 0; c < 7; c++) {
            String name = order[c].getDisplayName(TextStyle.SHORT, locale);
            name = cap(name);
            Label lbl = new Label(name);
            lbl.getStyleClass().add("weekday");
            GridPane.setMargin(lbl, new Insets(0, 0, 0, 2));
            weekdayHeader.add(lbl, c, 0);
        }
    }

    // Ricarica i dati prendendoli dal DB
    private void refresh() {
        // 1) Ricarica le statistiche dalla tabella plant_activity per il mese corrente
        reloadStatsForCurrentMonth();

        // 2) Ricostruisce la griglia (mese o settimana)
        if (monthBtn.isSelected()) {
            buildMonth();
        } else {
            buildWeek();
        }

        // 3) Aggiorna titolo pannello destro (mese o settimana)
        if (monthBtn.isSelected()) {
            rightTitle.setText(
                    cap(currentMonth.getMonth().getDisplayName(TextStyle.FULL, locale)) +
                            " " + currentMonth.getYear());
        } else {
            rightTitle.setText("Settimana di " + currentWeekStart);
        }

        // 4) Aggiorna lista piante del mese
        listMonth.setItems(computeMonthStats());
        updateSummary();
    }

    // Legge dal DB tutte le PlantActivity in un range che copre il mese corrente
    private void reloadStatsForCurrentMonth() {
        dailyStats.clear();
        if (repository == null)
            return;

        YearMonth month = currentMonth;
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();

        // Giorni margine extra, così copriamo anche le celle "grigie" prima/dopo il
        // mese
        LocalDate from = first.minusDays(7);
        LocalDate to = last.plusDays(7);

        List<PlantActivity> activities;
        try {
            activities = repository.getActivitiesBetween(from, to);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Aggrega per giorno -> (nome pianta -> minuti)
        Map<LocalDate, Map<String, Integer>> temp = new HashMap<>();
        for (PlantActivity a : activities) {
            LocalDate d = a.getDate();
            temp.computeIfAbsent(d, k -> new HashMap<>())
                    .merge(a.getPlantName(), a.getMinutes(), Integer::sum);
        }

        // Converte in lista ordinata per minuti (desc) per ogni giorno
        for (Map.Entry<LocalDate, Map<String, Integer>> entry : temp.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Integer> plantMap = entry.getValue();

            List<PlantStat> stats = plantMap.entrySet().stream()
                    .map(e -> new PlantStat(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparingInt((PlantStat p) -> p.minutes).reversed())
                    .collect(Collectors.toList());

            dailyStats.put(date, stats);
        }
    }

    // Costruzione griglia mese
    private void buildMonth() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().setAll(equalCols(7));
        calendarGrid.getRowConstraints().setAll(equalRows(6));

        LocalDate first = currentMonth.atDay(1);
        int shift = first.getDayOfWeek().getValue() - 1; // 0..6 (Lunedì = 1)
        LocalDate start = first.minusDays(shift);
        LocalDate today = LocalDate.now();

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                LocalDate date = start.plusDays(r * 7 + c);
                calendarGrid.add(makeDayCell(date, today, date.getMonth().equals(currentMonth.getMonth())), c, r);
            }
        }
        periodLabel.setText(
                cap(currentMonth.getMonth().getDisplayName(TextStyle.FULL, locale)) + " " + currentMonth.getYear());
    }

    private void buildWeek() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().setAll(equalCols(7));
        calendarGrid.getRowConstraints().setAll(equalRows(1));

        LocalDate today = LocalDate.now();
        for (int c = 0; c < 7; c++) {
            LocalDate d = currentWeekStart.plusDays(c);
            calendarGrid.add(makeDayCell(d, today, true), c, 0);
        }
        periodLabel.setText("Settimana di " + currentWeekStart);
    }

    /**
     * Crea la cella di un singolo giorno.
     * - Numero giorno
     * - Icona della pianta con più minuti (se esiste)
     * - Hover: nome + minuti
     * - Click: apre il dettaglio del giorno nel pannello destro
     */
    private Pane makeDayCell(LocalDate date, LocalDate today, boolean inMonth) {
        BorderPane cell = new BorderPane();
        cell.getStyleClass().add("day-cell");
        if (!inMonth)
            cell.getStyleClass().add("day-cell-out");
        if (date.equals(today))
            cell.getStyleClass().add("day-cell-today");

        Label day = new Label(String.valueOf(date.getDayOfMonth()));
        day.getStyleClass().add("day-number");
        BorderPane.setMargin(day, new Insets(6));

        ImageView plantImg = new ImageView();
        plantImg.setFitWidth(76);
        plantImg.setFitHeight(76);
        plantImg.setPreserveRatio(true);
        plantImg.setVisible(false);

        BorderPane.setMargin(plantImg, new Insets(4));

        // Recupera le statistiche di quel giorno dalla cache
        List<PlantStat> statsForDay = dailyStats.get(date);
        if (statsForDay != null && !statsForDay.isEmpty()) {
            // Pianta con più minuti = prima della lista (già ordinata desc)
            PlantStat top = statsForDay.get(0);

            Image img = loadIconForPlantName(top.name);
            if (img != null) {
                plantImg.setImage(img);
                plantImg.setVisible(true);
            }

            Tooltip tooltip = new Tooltip(top.name + " (" + top.minutes + " min)");
            Tooltip.install(cell, tooltip);
        }

        cell.setTop(day);

        cell.setCenter(plantImg);
        BorderPane.setMargin(plantImg, new Insets(4));

        cell.setMinSize(90, 110);

        cell.setOnMouseClicked(e -> showDayDetailsPopup(date));

        return cell;
    }

    private void showDayDetailsPopup(LocalDate date) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/application/canopy/view/daily_stats.fxml"));
            javafx.scene.Parent view = loader.load();

            DailyStatsController controller = loader.getController();
            // data e lista di stats
            List<PlantStat> stats = getStatsForDate(date);
            controller.setData(date, stats);

            javafx.scene.Scene scene = new javafx.scene.Scene(view, 900, 600);

            // 1. CARICAMENTO STILI GLOBALI
            scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());

            // 2. Caricamento Font
            com.application.canopy.model.FontManager.applyCurrentFont(scene);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Statistiche - " + date);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(scene);

            // 3. APPLICAZIONE CLASSE TEMA E SFONDO ROOT
            if (root.getScene() != null && root.getScene().getRoot() != null) {
                view.getStyleClass().setAll(root.getScene().getRoot().getStyleClass());
                view.getStyleClass().add("stats-root");
            } else {
                view.getStyleClass().add("theme-dark");
                view.getStyleClass().add("stats-root");
            }

            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Dettaglio sommario del mese (DESTRA)

    // Aggrega tutte le dailyStats del mese corrente in una lista di PlantStat:
    // una riga per pianta, con minuti sommati sul mese.
    private ObservableList<PlantStat> computeMonthStats() {
        Map<String, Integer> aggregated = new HashMap<>();

        for (Map.Entry<LocalDate, List<PlantStat>> entry : dailyStats.entrySet()) {
            LocalDate date = entry.getKey();
            if (!YearMonth.from(date).equals(currentMonth))
                continue;

            for (PlantStat ps : entry.getValue()) {
                aggregated.merge(ps.name, ps.minutes, Integer::sum);
            }
        }

        ObservableList<PlantStat> result = FXCollections.observableArrayList();
        aggregated.forEach((name, minutes) -> result.add(new PlantStat(name, minutes)));
        result.sort(Comparator.comparingInt((PlantStat p) -> p.minutes).reversed());
        return result;
    }

    private void updateSummary() {
        int total = listMonth.getItems().stream().mapToInt(p -> p.minutes).sum();
        summaryLabel.setText("Tot: " + total + " min");
    }

    // Recupera le statistiche di un singolo giorno.
    private List<PlantStat> getStatsForDate(LocalDate date) {
        List<PlantStat> stats = dailyStats.get(date);
        if (stats != null)
            return stats;

        if (repository == null)
            return Collections.emptyList();

        try {
            List<PlantActivity> activities = repository.getActivitiesForDate(date);
            if (activities.isEmpty()) {
                dailyStats.put(date, Collections.emptyList());
                return Collections.emptyList();
            }

            Map<String, Integer> agg = new HashMap<>();
            for (PlantActivity a : activities) {
                agg.merge(a.getPlantName(), a.getMinutes(), Integer::sum);
            }

            stats = agg.entrySet().stream()
                    .map(e -> new PlantStat(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparingInt((PlantStat p) -> p.minutes).reversed())
                    .collect(Collectors.toList());

            dailyStats.put(date, stats);
            return stats;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // Aggiunge minuti per una pianta su un giorno specifico
    public void addPlantMinutes(LocalDate date, String plantName, int minutes) {
        if (minutes <= 0 || repository == null)
            return;

        try {
            repository.addActivity(date, plantName, minutes);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        refresh();
    }

    // Carica l'icona per una pianta specifica
    private Image loadIconForPlantName(String plantName) {
        return com.application.canopy.util.ResourceManager.getPlantThumbnailByName(plantName);
    }

    // Helper per creare colonne con width uguale
    private ColumnConstraints[] equalCols(int n) {
        ColumnConstraints[] cols = new ColumnConstraints[n];
        for (int i = 0; i < n; i++) {
            var cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / n);
            cols[i] = cc;
        }
        return cols;
    }

    // Helper per creare righe con height uguale
    private RowConstraints[] equalRows(int n) {
        RowConstraints[] rows = new RowConstraints[n];
        for (int i = 0; i < n; i++) {
            var rc = new RowConstraints();
            rc.setPercentHeight(100.0 / n);
            rows[i] = rc;
        }
        return rows;
    }

    // Helper per creare stringhe con la prima lettera maiuscola
    private String cap(String s) {
        return s.substring(0, 1).toUpperCase(locale) + s.substring(1);
    }

    // Oggetto statistica pianta
    public static class PlantStat {
        public final String name;
        public final int minutes;

        public PlantStat(String name, int minutes) {
            this.name = name;
            this.minutes = minutes;
        }
    }

    // Oggetto cella da passare alla lista statistiche
    private static class PlantCell extends ListCell<PlantStat> {
        private final HBox root = new HBox(8);
        private final ImageView icon = new ImageView();
        private final Label name = new Label();
        private final Label mins = new Label();
        private final Region spacer = new Region();

        PlantCell() {
            icon.setFitWidth(20);
            icon.setFitHeight(20);
            icon.setPreserveRatio(true);
            icon.setVisible(false);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(icon, name, spacer, mins);
        }

        @Override
        protected void updateItem(PlantStat item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                name.setText(item.name);
                mins.setText(item.minutes + " min");
                setGraphic(root);
            }
        }
    }
}
