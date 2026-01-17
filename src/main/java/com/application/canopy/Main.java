package com.application.canopy;

import com.application.canopy.model.FontManager;
import com.application.canopy.model.ThemeManager;
import com.application.canopy.db.DatabaseManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        /* Inizializza SQLite */
        try {
            DatabaseManager.init();
            System.out.println("SQLite inizializzato!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Errore durante inizializzazione SQLite", e);
        }

        // Font e icona
        FontManager.initFonts();

        stage.getIcons().add(
                new javafx.scene.image.Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream(
                                        "/com/application/canopy/view/components/images/app/canopy.png"))));

        // Caricamento FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/application/canopy/view/app.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 720);

        // Caricamento CSS
        String[] styles = {
                "/css/base.css",
                "/css/achievements.css",
                "/css/calendar.css",
                "/css/herbarium.css"
        };

        for (String style : styles) {
            URL url = getClass().getResource(style);
            if (url == null) {
                throw new IllegalStateException("File CSS non trovato: " + style);
            }
            scene.getStylesheets().add(url.toExternalForm());
        }

        ThemeManager.applyTheme(root);
        FontManager.applyCurrentFont(scene);

        stage.setScene(scene);
        stage.setTitle("Canopy");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        DatabaseManager.close();
    }

    public static void main(String[] args) {

        System.setProperty("prism.order", "sw"); // forza renderer software
        System.setProperty("prism.text", "t2k"); // renderer testi alternativo

        launch(args);
    }
}
