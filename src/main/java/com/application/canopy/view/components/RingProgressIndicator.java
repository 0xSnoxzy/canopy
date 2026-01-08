package com.application.canopy.view.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Componente riutilizzabile per il progresso circolare.
 */
public class RingProgressIndicator extends StackPane {

    private final Arc arc;
    private final Label label;

    public RingProgressIndicator(int size, double thickness) {
        // BLOCCA dimensioni del nodo (fondamentale)
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        double radius = size / 2.0;

        Pane draw = new Pane();
        draw.setPrefSize(size, size);
        draw.setMinSize(size, size);
        draw.setMaxSize(size, size);

        // cerchio di fondo
        Circle track = new Circle(radius, radius, radius - thickness / 2);
        track.getStyleClass().add("ring-track");

        // cerchio interno (opzionale, per style)
        Circle inner = new Circle(radius, radius, radius - thickness - 2);
        inner.getStyleClass().add("ring-inner");

        // arco di progresso
        arc = new Arc(
                radius,
                radius,
                radius - thickness / 2,
                radius - thickness / 2,
                90,
                0);
        arc.setType(ArcType.OPEN);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.getStyleClass().add("ring-progress");

        draw.getChildren().addAll(track, inner, arc);

        label = new Label("0%");
        label.setMouseTransparent(true);

        getChildren().addAll(draw, label);

        // centra SEMPRE tutto nello StackPane
        setAlignment(draw, Pos.CENTER);
        setAlignment(label, Pos.CENTER);
    }

    public void setProgress(double value) {
        value = Math.max(0, Math.min(1, value));
        arc.setLength(-360 * value);
        label.setText(Math.round(value * 100) + "%");
    }

    public Label getLabel() {
        return label;
    }
}
