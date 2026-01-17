package com.application.canopy;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Navigator {

    private Navigator() {
    }

    // nome pagina -> FXML del contenuto (senza nav)
    private static final Map<String, String> ROUTES = Map.of(
            "home", "/com/application/canopy/view/home.fxml",
            "herbarium", "/com/application/canopy/view/herbarium.fxml",
            "achievements", "/com/application/canopy/view/achievements.fxml",
            "calendar", "/com/application/canopy/view/calendar.fxml",
            "settings", "/com/application/canopy/view/settings.fxml");

    private static final Map<String, Node> PAGES = new HashMap<>();

    private static StackPane contentRoot;

    public static void init(StackPane container) {
        contentRoot = container;
        // Nessun preloading massivo qui
    }

    public static void show(String route) {
        if (contentRoot == null)
            return;

        // 1. Cerca in cache
        Node page = PAGES.get(route);

        // 2. Se manca, carica ORA (Lazy Loading)
        if (page == null) {
            String fxmlPath = ROUTES.get(route);
            if (fxmlPath == null) {
                System.err.println("[Navigator] Rotta non trovata: " + route);
                return;
            }
            try {
                System.out.println("[Navigator] Caricamento di: " + route);
                FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
                page = loader.load();
                PAGES.put(route, page);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        contentRoot.getChildren().setAll(page);
    }

    // ----------------- FOCUS / FULLSCREEN API -----------------

    private static java.util.function.Consumer<Boolean> fullScreenListener;

    public static void setOnFullScreenToggle(java.util.function.Consumer<Boolean> listener) {
        fullScreenListener = listener;
    }

    public static void setFullScreen(boolean active) {
        if (fullScreenListener != null) {
            fullScreenListener.accept(active);
        }
    }
}
