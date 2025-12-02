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

    // ---------- PATH IMMAGINI ----------

    private static final String ROOT       = "/com/application/canopy/view/components/images/";
    private static final String THUMBS_DIR = ROOT + "thumbs/";

    /** id/nome -> base file immagini (lavanda -> lavanda.png, lavanda1.jpg, ...) */
    private static final Map<String, String> IMAGE_BASES = Map.of(
            "lavanda",            "lavanda",
            "menta",              "menta",
            "orchidea",           "orchidea",
            "peperoncino",        "peperoncino",
            "quercia",            "quercia",
            "radice_sussurrante", "radici_sussurranti",
            "lifeblood",          "lifeblood",
            "sakura",             "sakura"
    );

    // ---------- FXML: BOARD CENTRALE ----------

    @FXML private VBox  boardRoot;

    @FXML private Label plantTitle;
    @FXML private Label rarityBadge;
    @FXML private Text  plantCuriosity;
    @FXML private Text  plantDescription;
    @FXML private Text  plantCare;

    @FXML private ImageView plantImage;     // hero
    @FXML private ImageView variant1Image;  // var1
    @FXML private ImageView variant2Image;  // var2
    @FXML private ImageView variant3Image;  // var3


    @FXML private Label emptyHint;

    // ---------- FXML: SIDEBAR DESTRA ----------

    @FXML private TextField    searchField;
    @FXML private Button       clearSearchBtn;
    @FXML private ToggleButton allChip, commonChip, rareChip, specialChip;
    @FXML private ListView<PlantItem> plantsList;

    // dati
    private final ObservableList<PlantItem> source = FXCollections.observableArrayList();
    private FilteredList<PlantItem> filtered;

    private final GameState gameState = GameState.getInstance();

    // ---------- INITIALIZE ----------

    @FXML
    private void initialize() {
        loadFromGameState();

        if (plantsList != null) {
            plantsList.setCellFactory(lv -> new PlantCardCell());
            filtered = new FilteredList<>(source, p -> true);
            plantsList.setItems(filtered);
        }

        // toggle categorie
        ToggleGroup categoryGroup = new ToggleGroup();
        for (ToggleButton b : new ToggleButton[]{allChip, commonChip, rareChip, specialChip}) {
            b.setToggleGroup(categoryGroup);
        }
        allChip.setSelected(true);

        // ricerca
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        }
        if (clearSearchBtn != null) {
            clearSearchBtn.setOnAction(e -> searchField.clear());
        }
        categoryGroup.selectedToggleProperty().addListener((obs, o, n) -> applyFilters());

        // selezione lista
        if (plantsList != null) {
            plantsList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> showPlant(sel));
        }

        showEmptyState();
    }



    // ---------- COSTRUZIONE LISTA ----------

    private void loadFromGameState() {
        source.clear();
        for (UserPlantState state : gameState.getAllPlantStates()) {
            Plant p = state.getPlant();
            Category cat = classifyPlant(p);
            source.add(new PlantItem(
                    p,
                    p.getName(),
                    p.getDescription(),
                    p.getCuriosity(),
                    p.getCareTips(),
                    cat,
                    state.isUnlocked()
            ));
        }
    }

    private Category classifyPlant(Plant p) {
        String id = p.getId().toLowerCase(Locale.ROOT);
        if (id.contains("radice") || id.contains("lifeblood")) return Category.SPECIALE;
        if (id.contains("menta") || id.contains("quercia") || id.contains("peperoncino")) return Category.COMUNI;
        if (id.contains("orchidea") || id.contains("sakura") || id.contains("lavanda")) return Category.RARE;
        return Category.COMUNI;
    }

    private String categoryText(Category c) {
        return switch (c) {
            case COMUNI   -> "COMUNE";
            case RARE     -> "RARE";
            case SPECIALE -> "SPECIALE";
            case ALL      -> "";
        };
    }

    // ---------- FILTRI ----------

    private void applyFilters() {
        if (filtered == null) return;

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
        if (commonChip.isSelected())  set.add(Category.COMUNI);
        if (rareChip.isSelected())    set.add(Category.RARE);
        if (specialChip.isSelected()) set.add(Category.SPECIALE);
        if (set.isEmpty()) set.add(Category.ALL);
        return set;
    }

    // ---------- DETTAGLIO / BOARD ----------

    private void showPlant(PlantItem p) {
        if (p == null) {
            showEmptyState();
            return;
        }

        setBoardVisible(true);

        rarityBadge.setText(categoryText(p.category));

        if (!p.unlocked) {
            plantTitle.setText(p.name);
            plantCuriosity.setText("Sblocca questa pianta completando gli obiettivi.");
            plantDescription.setText("");
            plantCare.setText("");
            rarityBadge.setText("BLOCCATA");

            Image heroLocked = loadHeroImage(p.plant);
            plantImage.setImage(heroLocked);
            variant1Image.setImage(null);
            variant2Image.setImage(null);
            variant3Image.setImage(null);
            return;
        }

        plantTitle.setText(p.name);
        plantCuriosity.setText(safe(p.curiosity));
        plantDescription.setText(safe(p.description));
        plantCare.setText(safe(p.care));

        // HERO
        plantImage.setImage(loadHeroImage(p.plant));

        // VARIANTI
        variant1Image.setImage(loadVariantImage(p.plant, 1));
        variant2Image.setImage(loadVariantImage(p.plant, 2));
        variant3Image.setImage(loadVariantImage(p.plant, 3));
    }

    private void showEmptyState() {
        setBoardVisible(false);

        plantTitle.setText("-");
        plantCuriosity.setText("");
        plantDescription.setText("");
        plantCare.setText("");
        rarityBadge.setText("");

        plantImage.setImage(null);
        variant1Image.setImage(null);
        variant2Image.setImage(null);
        variant3Image.setImage(null);
    }

    private void setBoardVisible(boolean hasSelection) {
        if (boardRoot != null) {
            boardRoot.setVisible(hasSelection);
            boardRoot.setManaged(hasSelection);
        }
        if (emptyHint != null) {
            emptyHint.setVisible(!hasSelection);
            emptyHint.setManaged(!hasSelection);
        }
    }

    // ---------- CARICAMENTO IMMAGINI ----------

    private String imageBaseFor(Plant plant) {
        String id = plant.getId().toLowerCase(Locale.ROOT);

        for (var e : IMAGE_BASES.entrySet()) {
            if (id.contains(e.getKey())) return e.getValue();
        }

        String name = plant.getName().toLowerCase(Locale.ROOT);
        for (var e : IMAGE_BASES.entrySet()) {
            if (name.contains(e.getKey())) return e.getValue();
        }

        return id.replace(" ", "").replace("-", "_");
    }

    private Image loadHeroImage(Plant plant) {
        String base = imageBaseFor(plant);
        return loadFirstExisting(
                THUMBS_DIR + capitalizeFirst(base) + ".png",
                THUMBS_DIR + base + ".png",
                THUMBS_DIR + base + ".jpg"
        );
    }

    private Image loadVariantImage(Plant plant, int idx) {
        String base = imageBaseFor(plant);
        String suf = String.valueOf(idx);
        return loadFirstExisting(
                THUMBS_DIR + base + suf + ".png",
                THUMBS_DIR + base + suf + ".jpg"
        );
    }

    private Image loadThumbFor(Plant plant) {
        Image img = loadHeroImage(plant);
        if (img == null) {
            System.err.println("[Herbarium] Nessuna immagine trovata per " + plant.getName());
        }
        return img;
    }

    private Image loadFirstExisting(String... paths) {
        for (String path : paths) {
            Image img = loadImageFromResource(path);
            if (img != null) return img;
        }
        return null;
    }

    private Image loadImageFromResource(String path) {
        if (path == null) return null;
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("[Herbarium] Risorsa NON trovata: " + path);
            return null;
        }
        return new Image(url.toExternalForm(), false);
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---------- LISTVIEW CARD ----------

    private class PlantCardCell extends ListCell<PlantItem> {
        private final HBox      root    = new HBox(10);
        private final ImageView thumb   = new ImageView();
        private final VBox      textBox = new VBox(2);
        private final Label     title   = new Label();
        private final Label     subtitle = new Label();
        private final Pane      spacer  = new Pane();
        private final Label     lock    = new Label();

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
