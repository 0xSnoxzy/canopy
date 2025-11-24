package com.application.canopy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.application.canopy.model.ThemeManager;


public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Font (correzione path: niente src/main/resources qui)
        Font.loadFont(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/css/fonts/AtkinsonHyperlegible-Regular.ttf")
                ),
                14
        );

        stage.getIcons().add(
                new javafx.scene.image.Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream(
                                        "/com/application/canopy/view/components/images/app/canopy.png"
                                )
                        )
                )
        );

        // Carico il guscio con nav + centro vuoto
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/application/canopy/view/app.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1080, 620);

        URL css = getClass().getResource("/css/base-old.css");
        if (css == null) throw new IllegalStateException("File CSS non trovato.");
        scene.getStylesheets().add(css.toExternalForm());

        ThemeManager.applyTheme(root);

        stage.setScene(scene);
        stage.setTitle("Canopy");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
