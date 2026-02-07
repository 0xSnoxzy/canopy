package com.application.canopy.view.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.*;

//"Oggetto" riutilizzabile per il progresso circolare

public class RingProgressIndicator extends StackPane {

    private final Arc arc;
    private final Label label;

    public RingProgressIndicator(int size, double thickness) { //(dimezione, spessore dell'arco)
        // Blocca dimensioni
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        double radius = size / 2.0;
        //"foglio" in cui si disegnera
        Pane draw = new Pane();
        draw.setPrefSize(size, size);
        draw.setMinSize(size, size);
        draw.setMaxSize(size, size);

        // cerchio di fondo, il binario
        Circle track = new Circle(radius, radius, radius - thickness / 2);
        track.getStyleClass().add("ring-track");

        // cerchio interno, il buco tipo il buco della ciambella
        Circle inner = new Circle(radius, radius, radius - thickness - 2);
        inner.getStyleClass().add("ring-inner");

        // arco di progresso, quello che percorre il binario
        arc = new Arc(
                radius,
                radius,
                radius - thickness / 2,
                radius - thickness / 2,
                90, //90° da dove inizia
                0);//con quanto inizia per default
        arc.setType(ArcType.OPEN);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.getStyleClass().add("ring-progress");


        // cerchio di fondo
        draw.getChildren().addAll(track, inner, arc); //molto importante l'ordine

        label = new Label("0%");
        label.setMouseTransparent(true);

        getChildren().addAll(draw, label);

        // centra tutto nello StackPane
        setAlignment(draw, Pos.CENTER);
        setAlignment(label, Pos.CENTER);
    }

    //Qui si disegna il cercio dipendendo da vaule (il progresso)
    public void setProgress(double value) { //vaule é un numero che va da 0 a 1 (0.1=10% 0,5=50%...)
        value = Math.max(0, Math.min(1, value));//qui dice che se value <0 allora = 0 e se value >1 allora = 1
        arc.setLength(-360 * value);//il segno nei gradi ci da la direzione della barra
        label.setText(Math.round(value * 100) + "%");
    }

    //Qui mi ritorna il oggetto label permettendo poi che possa
    // aplicare delle modifiche sul label al livello di stile
    public Label getLabel() {
        return label;
    }
}
