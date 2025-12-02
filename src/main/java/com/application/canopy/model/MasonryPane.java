package com.application.canopy.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Layout "masonry" semplice:
 * - numero di colonne dipende dalla larghezza
 * - ogni child è una card che si adatta al proprio contenuto
 */
public class MasonryPane extends Pane {

    private final DoubleProperty hgap = new SimpleDoubleProperty(this, "hgap", 24);
    private final DoubleProperty vgap = new SimpleDoubleProperty(this, "vgap", 24);

    public MasonryPane() {
        widthProperty().addListener((obs, oldV, newV) -> requestLayout());
    }

    public double getHgap() {
        return hgap.get();
    }

    public void setHgap(double value) {
        hgap.set(value);
    }

    public DoubleProperty hgapProperty() {
        return hgap;
    }

    public double getVgap() {
        return vgap.get();
    }

    public void setVgap(double value) {
        vgap.set(value);
    }

    public DoubleProperty vgapProperty() {
        return vgap;
    }

    /** numero colonne "responsive" */
    private int computeColumns(double width) {
        if (width >= 900) return 3;
        if (width >= 600) return 2;
        return 1;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        if (width <= 0 || getChildren().isEmpty()) {
            return;
        }

        int cols = computeColumns(width);
        double hg = getHgap();
        double vg = getVgap();

        double totalGap = hg * (cols - 1);
        double colWidth = (width - totalGap) / cols;

        double[] colHeights = new double[cols];

        for (Node child : getChildren()) {
            if (!child.isManaged()) continue;

            // colonna con altezza minore
            int colIndex = 0;
            double min = colHeights[0];
            for (int i = 1; i < cols; i++) {
                if (colHeights[i] < min) {
                    min = colHeights[i];
                    colIndex = i;
                }
            }

            double x = colIndex * (colWidth + hg);
            double prefH = child.prefHeight(colWidth);
            double y = colHeights[colIndex];

            child.resizeRelocate(x, y, colWidth, prefH);

            colHeights[colIndex] = y + prefH + vg;
        }

        double maxHeight = 0;
        for (double h : colHeights) {
            if (h > maxHeight) maxHeight = h;
        }

        setMinHeight(maxHeight);
        setPrefHeight(maxHeight);
    }
}
