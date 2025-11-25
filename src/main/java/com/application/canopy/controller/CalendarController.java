package com.application.canopy.controller;

import com.application.canopy.db.DatabaseManager;
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

    @FXML private ToggleButton monthBtn, weekBtn;
    @FXML private Button prevBtn, nextBtn, backToMonthBtn;
    @FXML private Label periodLabel, rightTitle, summaryLabel;
    @FXML private GridPane calendarGrid, weekdayHeader;
    @FXML private ListView<PlantStat> listMonth, listDay;


    //addPlantMinutes(target, "Menta", 15);
    //addPlantMinutes(target, "Sakura", 90);

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
    private final Locale locale = Locale.ITALY;

    /** Repository che parla con SQLite. */
    private PlantActivityRepository repository;

    /**
     * Cache in memoria:
     * per ogni giorno, la lista di piante (già aggregate per minuti) di quel giorno.
     */
    private final Map<LocalDate, List<PlantStat>> dailyStats = new HashMap<>();

    @FXML
    private void initialize() {
        // Inizializza repository DB
        try {
            repository = new PlantActivityRepository(DatabaseManager.getConnection());
        } catch (SQLException e) {
            e.printStackTrace();
            // Se il DB non parte, il calendario funzionerà "vuoto"
        }

        buildWeekdayHeader();

        monthBtn.setOnAction(e -> {
            weekBtn.setSelected(false);
            backToMonth();
            refresh();
        });
        weekBtn.setOnAction(e -> {
            monthBtn.setSelected(false);
            backToMonth();
            refresh();
        });

        prevBtn.setOnAction(e -> {
            if (monthBtn.isSelected()) currentMonth = currentMonth.minusMonths(1);
            else currentWeekStart = currentWeekStart.minusWeeks(1);
            backToMonth();
            refresh();
        });
        nextBtn.setOnAction(e -> {
            if (monthBtn.isSelected()) currentMonth = currentMonth.plusMonths(1);
            else currentWeekStart = currentWeekStart.plusWeeks(1);
            backToMonth();
            refresh();
        });

        listMonth.setCellFactory(v -> new PlantCell());
        listDay.setCellFactory(v -> new PlantCell());

        backToMonthBtn.setOnAction(e -> backToMonth());

        // Primo caricamento
        refresh();
    }

    /* ======================
       BUILD HEADER / PERIODI
       ====================== */

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

    /**
     * Metodo centrale: ricarica i dati dal DB per il mese corrente,
     * ricostruisce la griglia (mese o settimana) e aggiorna pannello destro.
     */
    private void refresh() {
        // 1) Ricarica le statistiche dalla tabella plant_activity per il mese corrente
        reloadStatsForCurrentMonth();

        // 2) Ricostruisce la griglia (mese o settimana)
        if (monthBtn.isSelected()) {
            buildMonth();
        } else {
            buildWeek();
        }

        // 3) Aggiorna titolo pannello destro (mese / settimana)
        if (monthBtn.isSelected()) {
            rightTitle.setText(
                    cap(currentMonth.getMonth().getDisplayName(TextStyle.FULL, locale)) +
                            " " + currentMonth.getYear()
            );
        } else {
            rightTitle.setText("Settimana di " + currentWeekStart);
        }

        // 4) Aggiorna lista piante del mese (aggregata) + totale
        listMonth.setItems(computeMonthStats());
        updateSummary();
    }

    /**
     * Legge dal DB tutte le PlantActivity in un range che copre il mese corrente
     * (con qualche giorno extra ai bordi) e costruisce la mappa dailyStats.
     */
    private void reloadStatsForCurrentMonth() {
        dailyStats.clear();
        if (repository == null) return;

        YearMonth month = currentMonth;
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();

        // Aggiungo qualche giorno di margine, così copriamo bene anche le celle "grigie" prima/dopo il mese
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

    /* ======================
       COSTRUZIONE GRIGLIA
       ====================== */

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
                LocalDate date = start.plusDays(r * 7L + c);
                calendarGrid.add(makeDayCell(date, today, date.getMonth().equals(currentMonth.getMonth())), c, r);
            }
        }
        periodLabel.setText(
                cap(currentMonth.getMonth().getDisplayName(TextStyle.FULL, locale)) +
                        " " + currentMonth.getYear()
        );
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
     * - Tooltip con nome + minuti
     * - Click: apre il dettaglio del giorno nel pannello destro
     */
    private Pane makeDayCell(LocalDate date, LocalDate today, boolean inMonth) {
        BorderPane cell = new BorderPane();
        cell.getStyleClass().add("day-cell");
        if (!inMonth) cell.getStyleClass().add("day-cell-out");
        if (date.equals(today)) cell.getStyleClass().add("day-cell-today");

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

        cell.setOnMouseClicked(e -> showDayDetails(date));

        return cell;
    }

    /* ======================
       DETTAGLIO GIORNO / MESE
       ====================== */

    private void showDayDetails(LocalDate date) {


        rightTitle.setText("Piante del " + date);

        listMonth.setVisible(false);
        listMonth.setManaged(false);

        // Prende (o ricarica) le statistiche del giorno
        List<PlantStat> statsForDay = getStatsForDate(date);

        listDay.setItems(FXCollections.observableArrayList(statsForDay));
        listDay.setVisible(true);
        listDay.setManaged(true);

        int totalDay = statsForDay.stream().mapToInt(p -> p.minutes).sum();
        summaryLabel.setText("Tot: " + totalDay + " min");

        backToMonthBtn.setVisible(true);
        backToMonthBtn.setManaged(true);
    }

    private void backToMonth() {
        backToMonthBtn.setVisible(false);
        backToMonthBtn.setManaged(false);

        listDay.setVisible(false);
        listDay.setManaged(false);

        listMonth.setVisible(true);
        listMonth.setManaged(true);

        // Ripristina vista "mese" nel pannello destro
        listMonth.setItems(computeMonthStats());
        updateSummary();

        if (monthBtn.isSelected()) {
            rightTitle.setText(
                    cap(currentMonth.getMonth().getDisplayName(TextStyle.FULL, locale)) +
                            " " + currentMonth.getYear()
            );
        } else {
            rightTitle.setText("Settimana di " + currentWeekStart);
        }
    }

    /**
     * Aggrega tutte le dailyStats del mese corrente in una lista di PlantStat:
     * una riga per pianta, con minuti sommati sul mese.
     */
    private ObservableList<PlantStat> computeMonthStats() {
        Map<String, Integer> aggregated = new HashMap<>();

        for (Map.Entry<LocalDate, List<PlantStat>> entry : dailyStats.entrySet()) {
            LocalDate date = entry.getKey();
            if (!YearMonth.from(date).equals(currentMonth)) continue;

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

    /**
     * Recupera le statistiche di un singolo giorno.
     * Se non sono in cache, legge dal DB e popola dailyStats.
     */
    private List<PlantStat> getStatsForDate(LocalDate date) {
        List<PlantStat> stats = dailyStats.get(date);
        if (stats != null) return stats;

        if (repository == null) return Collections.emptyList();

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

    /* ======================
       API PER AGGIUNGERE MINUTI
       ====================== */

    /**
     * Metodo da chiamare DAL RESTO DELL'APP quando l'utente registra
     * tempo di cura per una pianta in una certa data.
     *
     * Esempio:
     * calendarController.addPlantMinutes(LocalDate.now(), "Menta", 20);
     */
    public void addPlantMinutes(LocalDate date, String plantName, int minutes) {
        if (minutes <= 0 || repository == null) return;

        try {
            repository.addActivity(date, plantName, minutes);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Dopo aver salvato sul DB, aggiorniamo la UI:
        refresh();
    }

    /* ======================
       SUPPORTO ICONA PIANTA
       ====================== */

    /**
     * Carica l'icona della pianta in base al nome.
     * Adatta questo metodo al tuo naming delle immagini.
     * Es: "Menta" -> /com/application/canopy/view/components/images/plants/menta.png
     */
    private Image loadIconForPlantName(String plantName) {
        if (plantName == null) return null;

        String key = plantName.toLowerCase(locale).trim();

        // Mappa tra nome pianta (come lo usi nell'app) e file nella cartella thumbs
        String fileName = switch (key) {
            case "lavanda"      -> "lavanda.png";
            case "menta"        -> "menta.png";
            case "orchidea"     -> "orchidea.png";
            case "peperoncino"  -> "peperoncino.png";
            case "quercia"      -> "Quercia.png"; // NOTA: nel tuo filesystem la Q è maiuscola
            case "sakura"       -> "sakura.png";
            default -> null; // nessuna icona conosciuta
        };

        if (fileName == null) {
            return null;
        }

        // Path reale delle immagini nel tuo progetto:
        // src/main/resources/com.application.canopy.view/components/images/thumbs/...
        String path = "/com/application/canopy/view/components/images/thumbs/" + fileName;

        var url = getClass().getResource(path);
        if (url != null) {
            return new Image(url.toExternalForm());
        }

        return null;
    }


    /* ======================
       LAYOUT HELPERS
       ====================== */

    private ColumnConstraints[] equalCols(int n) {
        ColumnConstraints[] cols = new ColumnConstraints[n];
        for (int i = 0; i < n; i++) {
            var cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / n);
            cols[i] = cc;
        }
        return cols;
    }

    private RowConstraints[] equalRows(int n) {
        RowConstraints[] rows = new RowConstraints[n];
        for (int i = 0; i < n; i++) {
            var rc = new RowConstraints();
            rc.setPercentHeight(100.0 / n);
            rows[i] = rc;
        }
        return rows;
    }

    private String cap(String s) {
        return s.substring(0, 1).toUpperCase(locale) + s.substring(1);
    }

    /* ======================
       DTO / CELL FACTORY
       ====================== */

    /** Statistica aggregata: nome pianta + minuti. */
    public static class PlantStat {
        public final String name;
        public final int minutes;

        public PlantStat(String name, int minutes) {
            this.name = name;
            this.minutes = minutes;
        }
    }

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
            icon.setVisible(false); // se in futuro vuoi metterci anche qui icone

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
