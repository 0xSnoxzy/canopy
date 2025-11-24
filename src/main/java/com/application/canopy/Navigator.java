package com.application.canopy;

import com.application.canopy.model.ThemeManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Navigator {

    private Navigator() {}

    // nome pagina -> FXML del contenuto (senza nav)
    private static final Map<String, String> ROUTES = Map.of(
            "home",         "/com/application/canopy/view/home.fxml",
            "herbarium",    "/com/application/canopy/view/herbarium.fxml",
            "achievements", "/com/application/canopy/view/achievements.fxml",
            "calendar",     "/com/application/canopy/view/calendar.fxml"
    );

    private static final Map<String, Node> PAGES = new HashMap<>();

    // contenitore centrale dove mostriamo le pagine
    private static StackPane contentRoot;

    // chiamato da AppController.initialize()
    public static void init(StackPane container) {
        contentRoot = container;

        // PRECARICO TUTTE LE PAGINE
        ROUTES.forEach((route, fxmlPath) -> {
            try {
                FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
                Node node = loader.load();

                PAGES.put(route, node);
            } catch (IOException e) {
                System.err.println("Errore caricando " + route + " (" + fxmlPath + ")");
                e.printStackTrace();
            }
        });
    }

    // mostra una pagina nel centro
    public static void show(String route) {
        if (contentRoot == null) return;

        Node page = PAGES.get(route);
        if (page == null) {
            System.err.println("Pagina non trovata: " + route);
            return;
        }

        contentRoot.getChildren().setAll(page);
    }
}
