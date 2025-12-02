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

        /* Inizializza SQLite*/
        try {
            DatabaseManager.init();
            System.out.println("SQLite inizializzato!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Errore durante inizializzazione SQLite", e);
        }

        // Font e icona come prima
        FontManager.initFonts();

        stage.getIcons().add(
                new javafx.scene.image.Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream(
                                        "/com/application/canopy/view/components/images/app/canopy.png"
                                )
                        )
                )
        );

        // Caricamento FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/application/canopy/view/app.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1080, 620);

        // Caricamento nuovi CSS
        String[] styles = {
                "/css/base.css",         // contiene temi + stile globale
                "/css/achievements.css", // schermata achievements
                "/css/calendar.css",     // calendario e dialog piante del giorno
                "/css/herbarium.css"     // erbario + glow selezione piante
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
        launch(args);
    }
}
