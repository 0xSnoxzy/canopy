package com.application.canopy.controller;

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
    public enum Category { ALL, COMUNI, RARE, SPECIALE }

    /** ViewModel per la lista dell’erbario, wrappa Plant + UserPlantState. */
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

    // --- immagini (coerente con la tua struttura) ---
    private static final String ROOT       = "/com/application/canopy/view/components/images/";
    private static final String THUMBS_DIR = ROOT + "thumbs/";
    private static final String HERO_DIR   = ROOT + "heroes/"; // opzionale: PNG grandi/trasparenti

    // --- FXML ---
    @FXML private BorderPane root;
    @FXML private NavController navController; // può rimanere null se non c'è nel FXML

    // Dettaglio / board
    @FXML private Label plantTitle;
    @FXML private Text plantCuriosity;
    @FXML private Text plantDescription;
    @FXML private Text plantCare;
    @FXML private Label emptyHint;

    @FXML private ImageView plantImage; // icona / hero principale
    @FXML private ImageView leafImage;  // immagine foglia (board tile)
    @FXML private ImageView fruitImage; // immagine frutto / seme (board tile)

    @FXML private ScrollPane detailScroll;
    @FXML private FlowPane detailFlow;  // board vera e propria (tile sparse)

    // Sidebar destra (lista + filtri)
    @FXML private TextField searchField;
    @FXML private Button clearSearchBtn;
    @FXML private ToggleButton allChip, commonChip, rareChip, specialChip;
    @FXML private ListView<PlantItem> plantsList;

    // dati
    private final ObservableList<PlantItem> source = FXCollections.observableArrayList();
    private FilteredList<PlantItem> filtered;

    private final GameState gameState = GameState.getInstance();

    @FXML
    private void initialize() {
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

        // Selezione pianta → dettaglio / board
        plantsList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> showPlant(sel));

        // Adatta la board alla larghezza del viewport
        if (detailScroll != null && detailFlow != null) {
            detailScroll.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
                double w = newV.getWidth();
                detailFlow.setPrefWidth(w);
                detailFlow.setPrefWrapLength(w - 32); // un po' di margine a destra/sinistra
            });
        }

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
                    p.getCuriosity(),
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
        if (id.contains("orchidea") || id.contains("sakura") || id.contains("lavanda")) return Category.RARE;
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

    // --- dettaglio / board -------------------------------------------------

    private void showPlant(PlantItem p) {
        if (p == null) {
            showEmptyState();
            return;
        }

        // testo base
        if (!p.unlocked) {
            setDetailVisible(true);
            plantTitle.setText(p.name + " (bloccata)");
            plantCuriosity.setText("Sblocca questa pianta completando gli obiettivi.");
            plantDescription.setText("");
            plantCare.setText("");

            if (plantImage != null) plantImage.setImage(loadThumbFor(p.plant));
            if (leafImage != null) leafImage.setImage(null);
            if (fruitImage != null) fruitImage.setImage(null);
            return;
        }

        setDetailVisible(true);
        plantTitle.setText(p.name);
        plantCuriosity.setText(safe(p.curiosity));
        plantDescription.setText(safe(p.description));
        plantCare.setText(safe(p.care));

        if (plantImage != null) {
            Image hero = loadHeroFor(p.plant);
            plantImage.setImage(hero != null ? hero : loadThumbFor(p.plant));
        }

        // per ora le tile foglia / frutto restano vuote;
        // in futuro potrai caricarle da risorse dedicate.
        if (leafImage != null)  leafImage.setImage(null);
        if (fruitImage != null) fruitImage.setImage(null);
    }

    private void showEmptyState() {
        setDetailVisible(false);
        plantTitle.setText("-");
        plantCuriosity.setText("");
        plantDescription.setText("");
        plantCare.setText("");

        if (plantImage != null) plantImage.setImage(null);
        if (leafImage != null)  leafImage.setImage(null);
        if (fruitImage != null) fruitImage.setImage(null);
    }

    private void setDetailVisible(boolean hasSelection) {
        if (emptyHint != null) {
            emptyHint.setVisible(!hasSelection);
            emptyHint.setManaged(!hasSelection);
        }
        if (detailFlow != null) {
            detailFlow.setVisible(hasSelection);
            detailFlow.setManaged(hasSelection);
        }
    }

    // --- caricamento immagini ---------------------------------------------

    private Image loadThumbFor(Plant plant) {
        String fileName = plant.getThumbFile(); // es: "Lavanda.png"
        String path = THUMBS_DIR + fileName;
        return loadImageFromResource(path);
    }

    /** Prova a caricare una PNG trasparente; fallback su thumb. */
    private Image loadHeroFor(Plant plant) {
        String file = tryGetHeroFileFromModel(plant);
        if (file != null) {
            Image img = loadImageFromResource(HERO_DIR + file);
            if (img != null) return img;

            // se l'attributo era già completo di path (caso raro), prova diretto
            img = loadImageFromResource(file);
            if (img != null) return img;
        }
        return loadThumbFor(plant);
    }

    private String tryGetHeroFileFromModel(Plant plant) {
        try {
            var m = plant.getClass().getMethod("getHeroFile");
            Object r = m.invoke(plant);
            if (r instanceof String s && !s.isBlank()) return s;
        } catch (ReflectiveOperationException ignored) { }

        try {
            var m = plant.getClass().getMethod("getHeroPngPath");
            Object r = m.invoke(plant);
            if (r instanceof String s && !s.isBlank()) return s;
        } catch (ReflectiveOperationException ignored) { }

        try {
            var m = plant.getClass().getMethod("getImageFile");
            Object r = m.invoke(plant);
            if (r instanceof String s && !s.isBlank()) return s;
        } catch (ReflectiveOperationException ignored) { }

        return null;
    }

    private Image loadImageFromResource(String path) {
        if (path == null) return null;
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("[Herbarium] Risorsa NON trovata: " + path);
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

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
