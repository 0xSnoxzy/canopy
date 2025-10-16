package com.application.canopy;

import com.application.canopy.controller.NavController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.Map;

public final class Navigator {

    private Navigator() {}

    // Mappa centralizzata delle route -> FXML
    private static final Map<String, String> ROUTES = Map.of(
            //"home",         "/com/application/canopy/view/home.fxml",
            //"herbarium",    "/com/application/canopy/view/herbarium.fxml"
            "achievements", "/com/application/canopy/view/achievements.fxml",
            "calendar",     "/com/application/canopy/view/calendar.fxml"

    );

    /** Cambia interamente la root della Scene con la pagina target */
    public static void go(BorderPane currentRoot, String route) {
        String fxml = ROUTES.get(route);
        if (fxml == null) return;

        try {
            Parent newRoot = FXMLLoader.load(Navigator.class.getResource(fxml));
            Scene scene = currentRoot.getScene();
            if (scene != null) {
                scene.setRoot(newRoot);
            } else {
                // fallback raro: se la Scene non è ancora pronta
                currentRoot.getChildren().setAll(newRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Collega il Nav della pagina corrente al Router e seleziona la voce attiva */
    public static void wire(NavController nav, BorderPane pageRoot, String activeRoute) {
        nav.setOnNavigate(route -> go(pageRoot, route));
        nav.setActive(activeRoute);
    }
}
