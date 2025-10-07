package com.application.canopy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("principale.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);

        URL css = getClass().getResource("/styles/theme.css");
        if (css == null) throw new IllegalStateException("CSS no encontrado");
        scene.getStylesheets().add(css.toExternalForm());


        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
}
