package com.application.canopy.controller;

import com.application.canopy.model.GameState;
import com.application.canopy.model.MasonryPane;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import javafx.geometry.Pos;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public class HerbariumController {

    // Nomi categorie piante
    public enum Category {
        ALL, COMUNI, RARE, SPECIALE
    }

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

    /** id/nome -> base file immagini (lavanda -> lavanda.png, lavanda1.jpg, ...) */
    private static final Map<String, String> IMAGE_BASES = Map.of(
            "lavanda", "lavanda",
            "menta", "menta",
            "orchidea", "orchidea",
            "peperoncino", "peperoncino",
            "quercia", "quercia",
            "radice_sussurrante", "radici_sussurranti",
            "lifeblood", "lifeblood",
            "sakura", "sakura");

    // Board centrale
    @FXML
    private StackPane herbRoot;
    @FXML
    private MasonryPane masonryBoard;

    @FXML
    private StackPane heroCard;
    @FXML
    private StackPane variant1Card;
    @FXML
    private StackPane variant2Card;
    @FXML
    private StackPane variant3Card;

    @FXML
    private Rectangle heroClip;
    @FXML
    private Rectangle variant1Clip;
    @FXML
    private Rectangle variant2Clip;
    @FXML
    private Rectangle variant3Clip;

    @FXML
    private Label plantTitle;
    @FXML
    private Label rarityBadge;
    @FXML
    private Text plantCuriosity;
    @FXML
    private Text plantDescription;
    @FXML
    private Text plantCare;

    @FXML
    private ImageView plantIcon;
    @FXML
    private ImageView variant1Image;
    @FXML
    private ImageView variant2Image;
    @FXML
    private ImageView variant3Image;

    @FXML
    private Label emptyHint;

    // Sidebar destra

    @FXML
    private TextField searchField;
    @FXML
    private Button clearSearchBtn;
    @FXML
    private ToggleButton allChip, commonChip, rareChip, specialChip;
    @FXML
    private ListView<PlantItem> plantsList;

    // dati
    private final ObservableList<PlantItem> source = FXCollections.observableArrayList();
    private FilteredList<PlantItem> filtered;

    private final GameState gameState = GameState.getInstance();

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
        for (ToggleButton b : new ToggleButton[] { allChip, commonChip, rareChip, specialChip }) {
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

        if (herbRoot != null && masonryBoard != null) {
            herbRoot.minHeightProperty().bind(masonryBoard.heightProperty());
            herbRoot.prefHeightProperty().bind(masonryBoard.heightProperty());
        }

        // Collegamento clip <-> card
        bindClipToCard(heroClip, heroCard);
        bindClipToCard(variant1Clip, variant1Card);
        bindClipToCard(variant2Clip, variant2Card);
        bindClipToCard(variant3Clip, variant3Card);

        // Collegamento immagine <-> card: riempie il rettangolo mantenendo il ratio
        bindImageToCard(plantIcon, heroCard);
        bindImageToCard(variant1Image, variant1Card);
        bindImageToCard(variant2Image, variant2Card);
        bindImageToCard(variant3Image, variant3Card);

        // Selezione automatica pianta al primo caricamento dell'erbario
        // Tramite il getter di gameState che prende la pianta caricata sulla home
        String lastId = gameState.getCurrentPlantId();
        PlantItem toSelect = null;

        if (lastId != null) {
            toSelect = source.stream()
                    .filter(item -> item.plant.getId().equals(lastId))
                    .findFirst().orElse(null);
        }

        // Se non ne trova prende la prima
        if (toSelect == null && !filtered.isEmpty()) {
            toSelect = filtered.get(0);
        }

        if (toSelect != null && plantsList != null) {
            plantsList.getSelectionModel().select(toSelect);
            plantsList.scrollTo(toSelect);
        } else {
            showEmptyState();
        }
    }

    private void bindClipToCard(Rectangle clip, Region card) {
        if (clip == null || card == null)
            return;
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
    }

    private void bindImageToCard(ImageView view, Region card) {
        if (view == null || card == null)
            return;
        view.fitWidthProperty().bind(card.widthProperty());
        // è l'aspect ratio a decidere l'altezza
        // view.fitHeightProperty().bind(card.heightProperty());
        view.setPreserveRatio(true);
        view.setSmooth(true);
    }

    // Costruzione della lista delle piante

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
                    state.isUnlocked()));
        }
    }

    private Category classifyPlant(Plant p) {
        String id = p.getId().toLowerCase(Locale.ROOT);
        if (id.contains("radice") || id.contains("lifeblood"))
            return Category.SPECIALE;
        if (id.contains("menta") || id.contains("quercia") || id.contains("peperoncino"))
            return Category.COMUNI;
        if (id.contains("orchidea") || id.contains("sakura") || id.contains("lavanda"))
            return Category.RARE;
        return Category.COMUNI;
    }

    private String categoryText(Category c) {
        return switch (c) {
            case COMUNI -> "COMUNE";
            case RARE -> "RARE";
            case SPECIALE -> "SPECIALE";
            case ALL -> "";
        };
    }

    // Filtri

    private void applyFilters() {
        if (filtered == null)
            return;

        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        EnumSet<Category> cats = selectedCategories();

        Predicate<PlantItem> pred = p -> {
            boolean categoryOk = cats.contains(Category.ALL) || cats.contains(p.category);
            if (!categoryOk)
                return false;
            if (q.isEmpty())
                return true;
            return (p.name + " " + p.description + " " + p.curiosity + " " + p.care)
                    .toLowerCase(Locale.ROOT)
                    .contains(q);
        };
        filtered.setPredicate(pred);
    }

    private EnumSet<Category> selectedCategories() {
        if (allChip.isSelected())
            return EnumSet.of(Category.ALL);
        EnumSet<Category> set = EnumSet.noneOf(Category.class);
        if (commonChip.isSelected())
            set.add(Category.COMUNI);
        if (rareChip.isSelected())
            set.add(Category.RARE);
        if (specialChip.isSelected())
            set.add(Category.SPECIALE);
        if (set.isEmpty())
            set.add(Category.ALL);
        return set;
    }

    // Dettagli della pianta selezionata

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

            Image iconLocked = loadPlantIconImage(p.plant);
            plantIcon.setImage(iconLocked);
            variant1Image.setImage(null);
            variant2Image.setImage(null);
            variant3Image.setImage(null);
            return;
        }

        plantTitle.setText(p.name);
        plantCuriosity.setText(safe(p.curiosity));
        plantDescription.setText(safe(p.description));
        plantCare.setText(safe(p.care));

        // icona principale
        plantIcon.setImage(loadPlantIconImage(p.plant));

        // Immagini varianti
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

        plantIcon.setImage(null);
        variant1Image.setImage(null);
        variant2Image.setImage(null);
        variant3Image.setImage(null);
    }

    private void setBoardVisible(boolean hasSelection) {
        if (masonryBoard != null) {
            masonryBoard.setVisible(hasSelection);
            masonryBoard.setManaged(hasSelection);
        }
        if (emptyHint != null) {
            emptyHint.setVisible(!hasSelection);
            emptyHint.setManaged(!hasSelection);
        }
    }

    // Caricamento delle immagini

    private String imageBaseFor(Plant plant) {
        String id = plant.getId().toLowerCase(Locale.ROOT);

        for (var e : IMAGE_BASES.entrySet()) {
            if (id.contains(e.getKey()))
                return e.getValue();
        }

        String name = plant.getName().toLowerCase(Locale.ROOT);
        for (var e : IMAGE_BASES.entrySet()) {
            if (name.contains(e.getKey()))
                return e.getValue();
        }

        return id.replace(" ", "").replace("-", "_");
    }

    private Image loadPlantIconImage(Plant plant) {
        String base = imageBaseFor(plant);

        return com.application.canopy.util.ResourceManager.loadFirstExisting(
                "/com/application/canopy/view/components/images/thumbs/" + capitalizeFirst(base) + ".png",
                "/com/application/canopy/view/components/images/thumbs/" + base + ".png",
                "/com/application/canopy/view/components/images/thumbs/" + base + ".jpg");
    }

    private Image loadVariantImage(Plant plant, int idx) {
        String base = imageBaseFor(plant);
        return com.application.canopy.util.ResourceManager.getPlantThumbVariant(base, String.valueOf(idx));
    }

    private Image loadThumbFor(Plant plant) {
        Image img = loadPlantIconImage(plant);
        if (img == null) {
            System.err.println("[Herbarium] Nessuna immagine trovata per " + plant.getName());
        }
        return img;
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // Lista delle piante a destra (celle)

    private class PlantCardCell extends ListCell<PlantItem> {
        private final HBox root = new HBox(10);
        private final StackPane iconContainer = new StackPane(); // Contenitore 40x40
        private final ImageView thumb = new ImageView();
        private final VBox textBox = new VBox(2);
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Pane spacer = new Pane();
        private final Label lock = new Label();

        PlantCardCell() {
            root.getStyleClass().add("card");
            root.getStyleClass().add("plant-card");
            root.setAlignment(Pos.CENTER_LEFT);

            iconContainer.setMinSize(40, 40);
            iconContainer.setPrefSize(40, 40);
            iconContainer.setMaxSize(40, 40);
            iconContainer.getChildren().add(thumb);

            thumb.setFitWidth(40);
            thumb.setFitHeight(40);
            thumb.setPreserveRatio(true);
            thumb.setSmooth(true);

            subtitle.getStyleClass().add("muted");

            HBox.setHgrow(spacer, Priority.ALWAYS);
            textBox.getChildren().addAll(title, subtitle);
            // textBox.setAlignment(Pos.CENTER_LEFT);

            root.getChildren().addAll(iconContainer, textBox, spacer, lock);

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
