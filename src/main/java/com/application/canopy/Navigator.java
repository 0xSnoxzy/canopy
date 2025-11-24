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
            "calendar",     "/com/application/canopy/view/calendar.fxml",
            "settings",     "/com/application/canopy/view/settings.fxml"
    );

    private static final Map<String, Node> PAGES = new HashMap<>();

    private static StackPane contentRoot;

    public static void init(StackPane container) {
        contentRoot = container;

        // precarico tutte le pagine
        ROUTES.forEach((route, fxmlPath) -> {
            try {
                FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
                Node node = loader.load();
                PAGES.put(route, node);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void show(String route) {
        if (contentRoot == null) return;

        Node page = PAGES.get(route);
        if (page == null) return;

        contentRoot.getChildren().setAll(page);
    }
}
