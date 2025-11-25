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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
    private static final String ROOT = "/com/application/canopy/view/components/images/";
    private static final String THUMBS_DIR = ROOT + "thumbs/";
    private static final String HERO_DIR   = ROOT + "heroes/"; // opzionale: PNG grandi/trasparenti
    private static final String STAGE_DIR  = ROOT + "stage/";  // sfondi selezionabili

    // --- FXML ---
    @FXML private BorderPane root;
    @FXML private NavController navController;

    // Stage centrale (NUOVO)
    @FXML private StackPane stageContainer;     // contenitore 16:9
    @FXML private ImageView stageBackground;    // sfondo selezionabile
    @FXML private ChoiceBox<String> backgroundChoice; // picker sfondi

    // Dettaglio testo
    @FXML private Label plantTitle;
    @FXML private Text plantCuriosity;
    @FXML private Text plantDescription;
    @FXML private Text plantCare;
    @FXML private Label emptyHint;

    // PNG pianta in overlay sullo stage (riuso il tuo fx:id)
    @FXML private ImageView plantImage;

    // Sidebar destra (lista + filtri)
    @FXML private TextField searchField;
    @FXML private Button clearSearchBtn;
    @FXML private ToggleButton allChip, commonChip, rareChip, specialChip;
    @FXML private ListView<PlantItem> plantsList;

    // Layout
    @FXML private SplitPane mainSplit;
    @FXML private ScrollPane detailScroll;
    @FXML private FlowPane detailFlow;

    private final ObservableList<PlantItem> source = FXCollections.observableArrayList();
    private FilteredList<PlantItem> filtered;

    private final GameState gameState = GameState.getInstance();

    // mappa etichetta -> path sfondo
    private final Map<String, String> bgMap = new LinkedHashMap<>();

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

        // Selezione pianta → dettaglio
        plantsList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> showPlant(sel));

        // --- STAGE: inizializzazione sicura (se presente nell'FXML) ---
        initStage();

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

    // --- dettaglio ---------------------------------------------------------

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
            // per bloccate: mostra solo thumb o nulla
            if (plantImage != null) plantImage.setImage(loadThumbFor(p.plant));
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
    }

    private void showEmptyState() {
        setDetailVisible(false);
        plantTitle.setText("-");
        plantCuriosity.setText("");
        plantDescription.setText("");
        plantCare.setText("");
        if (plantImage != null) plantImage.setImage(null);
    }

    private void setDetailVisible(boolean hasSelection) {
        if (emptyHint != null) {
            emptyHint.setVisible(!hasSelection);
            emptyHint.setManaged(!hasSelection);
        }
        if (stageContainer != null) {
            stageContainer.setVisible(hasSelection);
            stageContainer.setManaged(hasSelection);
        }
    }


    private Image loadThumbFor(Plant plant) {
        String fileName = plant.getThumbFile(); // es: "Lavanda.png"
        String path = THUMBS_DIR + fileName;
        return loadImageFromResource(path);
    }

    /** Prova a caricare una PNG trasparente; fallback su thumb. */
    private Image loadHeroFor(Plant plant) {
        // 1) Se il modello espone un file specifico (es. getHeroFile())
        String file = tryGetHeroFileFromModel(plant);
        if (file != null) {
            Image img = loadImageFromResource(HERO_DIR + file);
            if (img != null) return img;
            // se l'attributo era già completo di path (raro), prova diretto
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

    // --- stage

    private void initStage() {
        if (stageContainer == null || stageBackground == null || backgroundChoice == null || plantImage == null) {
            // lo stage non è presente in questo FXML → niente setup
            return;
        }

        stageContainer.widthProperty().addListener((o, ov, w) -> {
            double ww = w.doubleValue();
            stageContainer.setPrefHeight((ww * 9 / 16)/2);
            stageBackground.setFitWidth(ww/2);
            plantImage.setFitWidth(ww * 0.60);
        });

        bgMap.clear();
        bgMap.put("Studio — Soft", STAGE_DIR + "studio_soft.jpg");
        bgMap.put("Legno — Warm",  STAGE_DIR + "wood_warm.jpg");
        bgMap.put("Pietra — Cool", STAGE_DIR + "stone_cool.jpg");
        bgMap.put("Gradiente",     STAGE_DIR + "gradient.jpg");

        backgroundChoice.getItems().setAll(bgMap.keySet());
        backgroundChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            String path = bgMap.get(sel);
            Image bg = loadImageFromResource(path);
            stageBackground.setImage(bg);
        });
        // default
        backgroundChoice.getSelectionModel().selectFirst();
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

    private static String safe(String s) { return s == null ? "" : s; }
}
