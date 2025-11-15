package com.application.canopy.controller;

import com.application.canopy.Navigator;
import com.application.canopy.model.GameState;
import com.application.canopy.model.Plant;
import com.application.canopy.model.UserPlantState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.EnumSet;
import java.util.Locale;
import java.util.function.Predicate;

public class HerbariumController {

    // categorie solo per la UI
    public enum Category { ALL, COMUNI, RARE, SPECIALE}

    /**
     * ViewModel per la lista dell’erbario,
     * wrappa Plant + UserPlantState.
     */
    public static class PlantItem {
        public final Plant plant;
        public final String name;
        public final String description;
        public final String curiosity;
        public final String care;
        public final Category category;
        public final boolean unlocked;

        public PlantItem(Plant plant,
                         String name,
                         String description,
                         String curiosity,
                         String care,
                         Category category,
                         boolean unlocked) {
            this.plant = plant;
            this.name = name;
            this.description = description;
            this.curiosity = curiosity;
            this.care = care;
            this.category = category;
            this.unlocked = unlocked;
        }
    }

    // --- immagini (stessa struttura della home) ---
    private static final String ROOT = "/com/application/canopy/view/components/images/";
    private static final String THUMBS_DIR = ROOT + "thumbs/";

    // --- FXML ---

    @FXML private BorderPane root;
    @FXML private NavController navController;

    @FXML private Label plantTitle;
    @FXML private Text plantCuriosity;
    @FXML private Text plantDescription;
    @FXML private Text plantCare;
    @FXML private Label emptyHint;

    @FXML private ImageView plantImage;

    @FXML private TextField searchField;
    @FXML private Button clearSearchBtn;
    @FXML private ToggleButton allChip, commonChip, rareChip, specialChip;
    @FXML private ListView<PlantItem> plantsList;

    @FXML private SplitPane mainSplit;
    @FXML private ScrollPane detailScroll;
    @FXML private FlowPane detailFlow;

    private final ObservableList<PlantItem> source = FXCollections.observableArrayList();
    private FilteredList<PlantItem> filtered;

    private final GameState gameState = GameState.getInstance();

    @FXML
    private void initialize() {
        Navigator.wire(navController, root, "herbarium");

        // Popola l’erbario dal modello di gioco
        loadFromGameState();

        // ListView con card + thumb
        plantsList.setCellFactory(lv -> new PlantCardCell());
        filtered = new FilteredList<>(source, p -> true);
        plantsList.setItems(filtered);

        // Toggle chips categorie
        ToggleGroup categoryGroup = new ToggleGroup();
        for (ToggleButton b : new ToggleButton[]{allChip, commonChip, rareChip, specialChip}) {
            b.setToggleGroup(categoryGroup);
        }
        allChip.setSelected(true);

        // Ricerca + clear
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        clearSearchBtn.setOnAction(e -> searchField.clear());
        categoryGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> applyFilters());

        // Selezione pianta → dettaglio
        plantsList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> showPlant(sel));

        showEmptyState();
    }

    /** Costruisce la lista Herbarium leggendo da GameState */
    private void loadFromGameState() {
        source.clear();

        for (UserPlantState state : gameState.getAllPlantStates()) {
            Plant p = state.getPlant();
            Category cat = classifyPlant(p);

            PlantItem item = new PlantItem(
                    p,
                    p.getName(),
                    p.getDescription(),
                    p.getCuriosity(),      // uso il nome latino come curiosità
                    p.getCareTips(),
                    cat,
                    state.isUnlocked()
            );

            source.add(item);
        }
    }

    /** Mapping semplice Plant -> categoria UI */
    private Category classifyPlant(Plant p) {
        String id = p.getId().toLowerCase(Locale.ROOT);

        if (id.contains("radice") || id.contains("ceneradice")) return Category.SPECIALE;
        if (id.contains("menta") || id.contains("quercia") || id.contains("peperoncino")) return Category.COMUNI;
        if (id.contains("orchidea") || id.contains("sakura") || id.contains("lavanda")) return Category.RARE;;

        return Category.COMUNI;

    }

    // --- filtri / ricerca --------------------------------------------------

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
        if (commonChip.isSelected()) set.add(Category.COMUNI);
        if (rareChip.isSelected()) set.add(Category.RARE);
        if (specialChip.isSelected()) set.add(Category.SPECIALE);
        if (set.isEmpty()) set.add(Category.ALL);
        return set;
    }

    // --- dettaglio ---------------------------------------------------------

    private void showPlant(PlantItem p) {
        if (p == null) {
            showEmptyState();
            return;
        }

        emptyHint.setVisible(false);

        if (!p.unlocked) {
            plantTitle.setText(p.name + " (bloccata)");
            plantCuriosity.setText("Sblocca questa pianta completando gli obiettivi.");
            plantDescription.setText("");
            plantCare.setText("");
            if (plantImage != null) plantImage.setImage(loadThumbFor(p.plant)); // puoi anche metterla grigia
            return;
        }

        plantTitle.setText(p.name);
        plantCuriosity.setText(p.curiosity);
        plantDescription.setText(p.description);
        plantCare.setText(p.care);

        if (plantImage != null) {
            plantImage.setImage(loadThumbFor(p.plant));
        }
    }

    private void showEmptyState() {
        emptyHint.setVisible(true);
        plantTitle.setText("-");
        plantCuriosity.setText("");
        plantDescription.setText("");
        plantCare.setText("");
        if (plantImage != null) plantImage.setImage(null);
    }

    // --- immagini ----------------------------------------------------------

    private Image loadThumbFor(Plant plant) {
        String fileName = plant.getThumbFile(); // es: "Lavanda.png"
        String path = THUMBS_DIR + fileName;
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("[Herbarium] Thumb NON trovata: " + path);
            return null;
        }
        return new Image(url.toExternalForm(), true);
    }

    /** Cella ListView con thumb + lock, stile “card” minimal */
    private class PlantCardCell extends ListCell<PlantItem> {
        private final HBox root = new HBox(10);
        private final ImageView thumb = new ImageView();
        private final VBox textBox = new VBox(2);
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Pane spacer = new Pane();
        private final Label lock = new Label();

        PlantCardCell() {
            root.getStyleClass().add("card");
            root.getStyleClass().add("plant-card");

            thumb.setFitWidth(40);
            thumb.setFitHeight(40);
            thumb.setPreserveRatio(true);
            thumb.setSmooth(true);

            subtitle.getStyleClass().add("muted");

            HBox.setHgrow(spacer, Priority.ALWAYS);
            textBox.getChildren().addAll(title, subtitle);
            root.getChildren().addAll(thumb, textBox, spacer, lock);

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

                root.setOpacity(item.unlocked ? 1.0 : 0.5);
                thumb.setImage(loadThumbFor(item.plant));

                setGraphic(root);
            }
        }
    }
}
