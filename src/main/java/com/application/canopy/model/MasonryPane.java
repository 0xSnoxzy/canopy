package com.application.canopy.model;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

// Classe che estende Pane per creare un layout personalizzato in stile Pinterest
public class MasonryPane extends Pane {

    // spazio orizzontale tra le colonne
    private double hgap = 24;
    // spazio verticale tra le colonne
    private double vgap = 24;

    // Costruttore
    public MasonryPane() {
        // Aggiunge un listener alla larghezza del pannello
        // Quando la larghezza cambia, ricalcola il layout (requestLayout)
        widthProperty().addListener((obs, oldV, newV) -> requestLayout());
    }

    public double getHgap() {
        return hgap;
    }

    public void setHgap(double value) {
        hgap = value;
        // ricalcola il layout
        requestLayout();
    }

    public double getVgap() {
        return vgap;
    }

    public void setVgap(double value) {
        vgap = value;
        // ricalcola il layout
        requestLayout();
    }

    // calcola il numero di colonne in base alla larghezza disponibile
    private int computeColumns(double width) {
        // Se la larghezza è maggiore o uguale a 900px, usa 3 colonne
        if (width >= 900)
            return 3;
        // Se la larghezza è maggiore o uguale a 600px, usa 2 colonne
        if (width >= 600)
            return 2;
        // Altrimenti usa una sola colonna
        return 1;
    }

    @Override
    protected void layoutChildren() {
        // Ottiene la larghezza attuale del pannello
        double width = getWidth();

        // Se la larghezza è 0 o non ci sono figli, non fa nulla ed esce
        if (width <= 0 || getChildren().isEmpty()) {
            return;
        }

        // Calcola quante colonne usare in base alla larghezza corrente
        int cols = computeColumns(width);

        // Legge lo spazio orizzontale impostato
        double hg = getHgap();
        // Legge lo spazio verticale impostato
        double vg = getVgap();

        // Calcola lo spazio totale occupato dai gap orizzontali
        // Esempio: 3 colonne hanno 2 spazi tra di loro -> (3-1) * gap
        double totalGap = hg * (cols - 1);

        // Calcola la larghezza di ogni singola colonna sottraendo i gap alla larghezza totale
        double colWidth = (width - totalGap) / cols;

        // Crea un array per tenere traccia dell'altezza corrente di ogni colonna
        double[] colHeights = new double[cols];

        // Itera su tutti i figli aggiunti al Pane
        for (Node child : getChildren()) {
            // Se il figlio non è gestito lo salta
            if (!child.isManaged())
                continue;

            // trova la colonna più corta dove inserire il prossimo elemento (algoritmo di min base)
            int colIndex = 0; // Indice della colonna candidata
            double min = colHeights[0]; // Altezza minima trovata finora

            // Scorre le altre colonne per vedere se ce n'è una più bassa
            for (int i = 1; i < cols; i++) {
                if (colHeights[i] < min) {
                    min = colHeights[i]; // Aggiorna l'altezza minima
                    colIndex = i; // Salva l'indice della colonna più bassa
                }
            }

            // Calcola la posizione X: indice colonna * (larghezza colonna + spazio)
            double x = colIndex * (colWidth + hg);

            // Calcola l'altezza preferita del figlio dato che la larghezza è fissata
            double prefH = child.prefHeight(colWidth);

            // La posizione Y è l'altezza attuale della colonna scelta
            double y = colHeights[colIndex];

            // Posiziona e ridimensiona il figlio alle coordinate calcolate
            child.resizeRelocate(x, y, colWidth, prefH);

            // Aggiorna l'altezza della colonna scelta aggiungendo l'altezza del figlio + il gap verticale
            // La prossima card in questa colonna andrà sotto questa
            colHeights[colIndex] = y + prefH + vg;
        }

        // Dopo aver piazzato tutti i figli, calcola l'altezza massima raggiunta tra le colonne
        double maxHeight = 0;
        for (double h : colHeights) {
            if (h > maxHeight)
                maxHeight = h;
        }

        // Imposta l'altezza del pannello MasonryPane per contenere tutte le colonne
        // Questo permette allo scrollpane padre di scorrere correttamente
        setMinHeight(maxHeight);
        setPrefHeight(maxHeight);
    }
}
