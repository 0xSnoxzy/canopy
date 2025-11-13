package com.application.canopy.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;

import com.application.canopy.Navigator;
import javafx.scene.layout.BorderPane;

public class CalendarController {
    // Navigazione
    @FXML private BorderPane root;
    @FXML private NavController navController;

    @FXML private ToggleButton monthBtn, weekBtn;
    @FXML private Button prevBtn, nextBtn, backToMonthBtn;
    @FXML private Label periodLabel, rightTitle, summaryLabel;
    @FXML private GridPane calendarGrid, weekdayHeader;
    @FXML private ListView<PlantStat> listMonth, listDay;

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
    private final Locale locale = Locale.ITALY;

    @FXML
    private void initialize() {
        Navigator.wire(navController, root, "calendar");
        buildWeekdayHeader();

        monthBtn.setOnAction(e -> { weekBtn.setSelected(false); backToMonth(); refresh(); });
        weekBtn.setOnAction(e -> { monthBtn.setSelected(false); backToMonth(); refresh(); });

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

        // Celle pronte per immagine futura
        listMonth.setCellFactory(v -> new PlantCell());
        listDay.setCellFactory(v -> new PlantCell());

        // Dati demo MENSILI (restano quelli che avevi)
        listMonth.setItems(FXCollections.observableArrayList(
                new PlantStat("Quercia", 320),
                new PlantStat("Girasole", 280),
                new PlantStat("Acero", 150)
        ));
        updateSummary(); // Tot minuti

        backToMonthBtn.setOnAction(e -> backToMonth());

        refresh();
    }

    private void buildWeekdayHeader() {
        weekdayHeader.getChildren().clear();
        weekdayHeader.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            weekdayHeader.getColumnConstraints().add(cc);
        }
        DayOfWeek[] order = { DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY };
        for (int c = 0; c < 7; c++) {
            String name = order[c].getDisplayName(TextStyle.SHORT, locale);
            name = cap(name);
            Label lbl = new Label(name);
            lbl.getStyleClass().add("weekday");
            GridPane.setMargin(lbl, new Insets(0, 0, 0, 2));
            weekdayHeader.add(lbl, c, 0);
        }
    }

    private void refresh() {
        if (monthBtn.isSelected()) buildMonth(); else buildWeek();
        if (monthBtn.isSelected()) {
            rightTitle.setText(cap(currentMonth.getMonth().getDisplayName(TextStyle.FULL, locale)) + " " + currentMonth.getYear());
        } else {
            rightTitle.setText("Settimana di " + currentWeekStart);
        }
    }

    private void buildMonth() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().setAll(equalCols(7));
        calendarGrid.getRowConstraints().setAll(equalRows(6));

        LocalDate first = currentMonth.atDay(1);
        int shift = first.getDayOfWeek().getValue() - 1; // 0..6
        LocalDate start = first.minusDays(shift);
        LocalDate today = LocalDate.now();

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                LocalDate date = start.plusDays(r * 7L + c);
                calendarGrid.add(makeDayCell(date, today, date.getMonth().equals(currentMonth.getMonth())), c, r);
            }
        }
        periodLabel.setText(cap(currentMonth.getMonth().getDisplayName(TextStyle.FULL, locale)) + " " + currentMonth.getYear());
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

    private Pane makeDayCell(LocalDate date, LocalDate today, boolean inMonth) {
        BorderPane cell = new BorderPane();
        cell.getStyleClass().add("day-cell");
        if (!inMonth) cell.getStyleClass().add("day-cell-out");
        if (date.equals(today)) cell.getStyleClass().add("day-cell-today");

        Label day = new Label(String.valueOf(date.getDayOfMonth()));
        day.getStyleClass().add("day-number");
        BorderPane.setMargin(day, new Insets(6));

        ImageView plantImg = new ImageView(); // placeholder per futuro
        plantImg.setFitWidth(36); plantImg.setFitHeight(36);
        plantImg.setPreserveRatio(true);
        plantImg.setVisible(false);
        BorderPane.setMargin(plantImg, new Insets(0, 6, 6, 0));

        cell.setTop(day);
        cell.setBottom(plantImg);
        cell.setMinSize(90, 80);

        // Click: mostra lista del giorno nel pannello destro
        cell.setOnMouseClicked(e -> showDayDetails(date));

        return cell;
    }

    private void showDayDetails(LocalDate date) {
        rightTitle.setText("Piante del " + date);
        listMonth.setVisible(false);
        listMonth.setManaged(false);

        // Nessun dato per ora (placeholder vuoto)
        listDay.setItems(FXCollections.observableArrayList());
        listDay.setVisible(true);
        listDay.setManaged(true);

        // Totale giorno (0 per ora)
        summaryLabel.setText("Tot: 0 min");

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

        updateSummary();

        if (monthBtn.isSelected()) {
            rightTitle.setText(cap(currentMonth.getMonth().getDisplayName(TextStyle.FULL, locale)) + " " + currentMonth.getYear());
        } else {
            rightTitle.setText("Settimana di " + currentWeekStart);
        }
    }

    private void updateSummary() {
        int total = listMonth.getItems().stream().mapToInt(p -> p.minutes).sum();
        summaryLabel.setText("Tot: " + total + " min");
    }

    private ColumnConstraints[] equalCols(int n) {
        ColumnConstraints[] cols = new ColumnConstraints[n];
        for (int i = 0; i < n; i++) { var cc = new ColumnConstraints(); cc.setPercentWidth(100.0 / n); cols[i] = cc; }
        return cols;
    }
    private RowConstraints[] equalRows(int n) {
        RowConstraints[] rows = new RowConstraints[n];
        for (int i = 0; i < n; i++) { var rc = new RowConstraints(); rc.setPercentHeight(100.0 / n); rows[i] = rc; }
        return rows;
    }
    private String cap(String s){ return s.substring(0,1).toUpperCase(locale) + s.substring(1); }


    public static class PlantStat {
        public final String name;
        public final int minutes;
        public PlantStat(String name, int minutes) { this.name = name; this.minutes = minutes; }
    }

    private static class PlantCell extends ListCell<PlantStat> {
        private final HBox root = new HBox(8);
        private final ImageView icon = new ImageView();
        private final Label name = new Label();
        private final Label mins = new Label();
        private final Region spacer = new Region();

        PlantCell() {
            icon.setFitWidth(20); icon.setFitHeight(20); icon.setPreserveRatio(true);
            icon.setVisible(false); // immagini in futuro
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(icon, name, spacer, mins);
        }
        @Override
        protected void updateItem(PlantStat item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setGraphic(null);
            else {
                name.setText(item.name);
                mins.setText(item.minutes + " min");
                setGraphic(root);
            }
        }
    }
}
