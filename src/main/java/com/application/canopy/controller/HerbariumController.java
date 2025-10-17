package com.application.canopy.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import java.util.EnumSet;
import java.util.Locale;
import java.util.function.Predicate;

import com.application.canopy.Navigator;

public class HerbariumController {

    public enum Category { ALL, INDOOR, OUTDOOR, SUCCULENT, HERB }

    public static class PlantItem {
        public final String name;
        public final String description;
        public final String curiosity;
        public final String care;
        public final Category category;
        public final boolean unlocked;

        public PlantItem(String name, String description, String curiosity, String care,
                         Category category, boolean unlocked) {
            this.name = name;
            this.description = description;
            this.curiosity = curiosity;
            this.care = care;
            this.category = category;
            this.unlocked = unlocked;
        }
    }

    // --- Riferimenti FXML ---

    @FXML private BorderPane root;
    @FXML private NavController navController;

    @FXML private Label plantTitle;
    @FXML private Text plantCuriosity;
    @FXML private Text plantDescription;
    @FXML private Text plantCare;
    @FXML private Label emptyHint;

    @FXML private TextField searchField;
    @FXML private Button clearSearchBtn;
    @FXML private ToggleButton allChip, indoorChip, outdoorChip, succulentChip, herbChip;
    @FXML private ListView<PlantItem> plantsList;

    @FXML private SplitPane mainSplit;
    @FXML private ScrollPane detailScroll;
    @FXML private FlowPane detailFlow;

    private final ObservableList<PlantItem> source = FXCollections.observableArrayList();
    private FilteredList<PlantItem> filtered;



    @FXML
    private void initialize() {
        // --- Dati di esempio ---

        Navigator.wire(navController, root, "herbarium");
        source.addAll(
                new PlantItem("Aloe Vera", "Pianta succulenta dalle foglie carnose.",
                        "Gel lenitivo usato da secoli.", "Poca acqua, molta luce.",
                        Category.SUCCULENT, true),
                new PlantItem("Basilico", "Erba aromatica annuale.",
                        "Simbolo di amore in alcune culture.", "Luce e terreno umido.",
                        Category.HERB, true),
                new PlantItem("Monstera", "Pianta tropicale da interno.",
                        "Le foglie diventano forate crescendo.", "Luce indiretta e umidità.",
                        Category.INDOOR, false)
        );

        // Imposta lista
        plantsList.setCellFactory(lv -> new PlantCardCell());
        filtered = new FilteredList<>(source, p -> true);
        plantsList.setItems(filtered);

        // Ricerca + filtri
        var categoryGroup = new ToggleGroup();
        for (var b : new ToggleButton[]{allChip, indoorChip, outdoorChip, succulentChip, herbChip})
            b.setToggleGroup(categoryGroup);
        allChip.setSelected(true);

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        clearSearchBtn.setOnAction(e -> searchField.clear());
        categoryGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> applyFilters());

        // Selezione pianta -> mostra dettagli
        plantsList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> showPlant(sel));

        showEmptyState();
    }

    private void applyFilters() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        EnumSet<Category> cats = selectedCategories();

        Predicate<PlantItem> pred = p -> {
            boolean categoryOk = cats.contains(Category.ALL) || cats.contains(p.category);
            if (!categoryOk) return false;

            if (q.isEmpty()) return true;
            return (p.name + " " + p.description + " " + p.curiosity + " " + p.care)
                    .toLowerCase(Locale.ROOT)
                    .contains(q);
        };

        filtered.setPredicate(pred);
    }

    private EnumSet<Category> selectedCategories() {
        if (allChip.isSelected()) return EnumSet.of(Category.ALL);
        EnumSet<Category> set = EnumSet.noneOf(Category.class);
        if (indoorChip.isSelected()) set.add(Category.INDOOR);
        if (outdoorChip.isSelected()) set.add(Category.OUTDOOR);
        if (succulentChip.isSelected()) set.add(Category.SUCCULENT);
        if (herbChip.isSelected()) set.add(Category.HERB);
        if (set.isEmpty()) set.add(Category.ALL);
        return set;
    }

    private void showPlant(PlantItem p) {
        if (p == null) { showEmptyState(); return; }

        emptyHint.setVisible(false);
        plantTitle.setText(p.name);
        plantCuriosity.setText(p.curiosity);
        plantDescription.setText(p.description);
        plantCare.setText(p.care);

        // QUI IN FUTURO POTRAI ANCHE CAMBIARE L’IMMAGINE DELLA PIANTA SE INSERITA
        // Esempio:
        // plantImage.setImage(new Image(getClass().getResourceAsStream(p.imagePath)));
    }

    private void showEmptyState() {
        emptyHint.setVisible(true);
        plantTitle.setText("—");
        plantCuriosity.setText("");
        plantDescription.setText("");
        plantCare.setText("");
        // QUI IN FUTURO POTRAI MOSTRARE UN PLACEHOLDER IMMAGINE
    }

    /** Cella personalizzata per la lista di piante */
    private static class PlantCardCell extends ListCell<PlantItem> {
        private final HBox root = new HBox(12);
        private final VBox textBox = new VBox(2);
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Pane spacer = new Pane();
        private final Label lock = new Label();

        PlantCardCell() {
            root.getStyleClass().add("card");
            subtitle.getStyleClass().add("muted");
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(textBox, spacer, lock);
            textBox.getChildren().addAll(title, subtitle);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(PlantItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                title.setText(item.name);
                subtitle.setText(item.category.name());
                lock.setText(item.unlocked ? "" : "🔒");
                setGraphic(root);
            }
        }
    }
}
