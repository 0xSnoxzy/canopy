package com.application.canopy.controller;

import com.application.canopy.controller.CalendarController.PlantStat;
import com.application.canopy.db.PlantActivityRepository;
import com.application.canopy.model.PlantActivity;
import com.application.canopy.model.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DailyStatsController {

    @FXML
    private BorderPane root;
    @FXML
    private Label dateTitle;
    @FXML
    private PieChart pieChart;
    @FXML
    private LineChart<String, Number> lineChart;
    @FXML
    private ListView<PlantStat> detailsList;

    private PlantActivityRepository repository;

    @FXML
    public void initialize() {
        repository = com.application.canopy.service.ServiceLocator.getInstance().getPlantActivityRepository();

        // Custom cell factory for the list
        detailsList.setCellFactory(lv -> new ListCell<>() {
            private final HBox root = new HBox(10);
            private final Label name = new Label();
            private final Region spacer = new Region();
            private final Label mins = new Label();

            {
                name.getStyleClass().add("stat-item-name");
                mins.getStyleClass().add("stat-item-value");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                root.getChildren().addAll(name, spacer, mins);
                root.getStyleClass().add("stat-item");
            }

            @Override
            protected void updateItem(PlantStat item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    name.setText(item.name);
                    mins.setText(item.minutes + " min");

                    // Lookup color
                    Map<String, String> plantColors = com.application.canopy.model.GameState.getInstance()
                            .getAllPlants().stream()
                            .collect(Collectors.toMap(com.application.canopy.model.Plant::getName,
                                    com.application.canopy.model.Plant::getColor));
                    String color = plantColors.get(item.name);
                    if (color != null) {
                        name.setStyle("-fx-text-fill: " + color + ";");
                    } else {
                        name.setStyle("");
                    }

                    setGraphic(root);
                }
            }
        });

        // Apply theme if needed (though CSS should handle it)
        ThemeManager.applyTheme(root);
    }

    public void setData(LocalDate date, List<PlantStat> dayStats) {
        // 1. Title
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.ITALY);
        String text = date.format(fmt);
        // Capitalize first letter
        text = text.substring(0, 1).toUpperCase() + text.substring(1);
        dateTitle.setText(text);

        // 2. Pie Chart (Distribution of plants for THAT day)
        populatePieChart(dayStats);

        // 3. Line Chart (Weekly Trend ending on THAT day)
        populateLineChart(date);

        // 4. List View
        detailsList.setItems(FXCollections.observableArrayList(dayStats));
    }

    private void populatePieChart(List<PlantStat> stats) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (PlantStat ps : stats) {
            pieData.add(new PieChart.Data(ps.name, ps.minutes));
        }
        pieChart.setData(pieData);

        // Map plant names to their colors
        Map<String, String> plantColors = com.application.canopy.model.GameState.getInstance().getAllPlants().stream()
                .collect(Collectors.toMap(com.application.canopy.model.Plant::getName,
                        com.application.canopy.model.Plant::getColor));

        // Apply colors to chart slices
        for (PieChart.Data data : pieChart.getData()) {
            String color = plantColors.get(data.getName());
            if (color != null) {
                // Apply color to the slice node
                javafx.scene.Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + color + ";");
                }
            }
        }
    }

    private void populateLineChart(LocalDate targetDate) {
        // We want the last 7 days including targetDate
        LocalDate startDate = targetDate.minusDays(6);

        // Clear previous data
        lineChart.getData().clear();

        try {
            // Get all activities in range
            List<PlantActivity> activities = repository.getActivitiesBetween(startDate, targetDate);

            // Get colors
            Map<String, String> plantColors = com.application.canopy.model.GameState.getInstance().getAllPlants()
                    .stream()
                    .collect(Collectors.toMap(com.application.canopy.model.Plant::getName,
                            com.application.canopy.model.Plant::getColor));

            // Identify active plants in this period
            Set<String> activePlants = activities.stream()
                    .map(PlantActivity::getPlantName)
                    .collect(Collectors.toSet());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

            // Create a series for each active plant
            for (String plantName : activePlants) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(plantName);

                // Populate data for 7 days (filling 0s if missing)
                for (int i = 0; i < 7; i++) {
                    LocalDate d = startDate.plusDays(i);
                    // Sum minutes for this plant on this day
                    int min = activities.stream()
                            .filter(a -> a.getDate().equals(d) && a.getPlantName().equals(plantName))
                            .mapToInt(PlantActivity::getMinutes)
                            .sum();

                    String label = d.format(formatter);
                    XYChart.Data<String, Number> data = new XYChart.Data<>(label, min);
                    series.getData().add(data);
                }

                // Add series to chart
                lineChart.getData().add(series);

                // Apply style (color)
                String color = plantColors.get(plantName);
                if (color != null) {
                    // We must wait for the node to be created
                    if (series.getNode() != null) {
                        series.getNode().setStyle("-fx-stroke: " + color + ";");
                    } else {
                        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
                            if (newNode != null) {
                                newNode.setStyle("-fx-stroke: " + color + ";");
                            }
                        });
                    }

                    // Also style the legend symbol if possible (tricky without lookup)
                    // And data points
                    for (XYChart.Data<String, Number> data : series.getData()) {
                        if (data.getNode() != null) {
                            data.getNode().setStyle("-fx-background-color: " + color + ", white;");
                        } else {
                            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                                if (newNode != null) {
                                    newNode.setStyle("-fx-background-color: " + color + ", white;");
                                }
                            });
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
