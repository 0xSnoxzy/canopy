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

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Font.loadFont(getClass().getResourceAsStream("src/main/resources/css/fonts/AtkinsonHyperlegible-Regular.ttf"), 14);

        stage.getIcons().add(
                new javafx.scene.image.Image(
                        Objects.requireNonNull(getClass().getResourceAsStream("/com/application/canopy/view/components/images/app/canopy.png"))
                )
        );


        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/application/canopy/view/home.fxml")));
        Scene scene = new Scene(root, 1080, 620);

        URL css = getClass().getResource("/css/base.css");

        if (css == null) throw new IllegalStateException("File CSS non trovato.");
        scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Canopy");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
