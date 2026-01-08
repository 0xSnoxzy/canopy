package com.application.canopy.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.util.Duration;

/**
 * Gestisce la logica del timer Pomodoro: stati, fasi, conteggio del tempo.
 * Non dipende dalla UI.
 */
public class PomodoroTimerService {

    public enum TimerState {
        IDLE, RUNNING
    }

    public enum Phase {
        FOCUS, BREAK
    }

    private final ObjectProperty<TimerState> timerState = new SimpleObjectProperty<>(TimerState.IDLE);
    private final ObjectProperty<Phase> phase = new SimpleObjectProperty<>(Phase.FOCUS);

    // Tempo rimanente nel blocco corrente
    private final IntegerProperty remainingSeconds = new SimpleIntegerProperty(0);
    // Tempo totale del blocco corrente (per calcolare %)
    private final IntegerProperty totalSeconds = new SimpleIntegerProperty(0);

    // Tempo totale della sessione (somma di tutti i blocchi pianificati)
    private final IntegerProperty sessionTotalSeconds = new SimpleIntegerProperty(0);
    // Tempo trascorso in sessione
    private final IntegerProperty sessionElapsedSeconds = new SimpleIntegerProperty(0);

    // Parametri di configurazione corrente
    private int focusMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int longBreakInterval = 4;
    private boolean breaksEnabled = true;

    // Stato avanzamento cicli
    private int totalCycles = 1;
    private int completedCycles = 0;

    private Timeline timeline;
    private Runnable onPomodoroCompleted; // Callback per quando finisce un pomodoro completato

    public PomodoroTimerService() {
    }

    // ----------------- CONFIGURAZIONE -----------------

    public void configureSession(int focusMins, int shortBreakMins, int longBreakMins,
            int cycles, boolean enableBreaks, int longBreakInt) {
        this.focusMinutes = focusMins;
        this.shortBreakMinutes = shortBreakMins;
        this.longBreakMinutes = longBreakMins;
        this.totalCycles = cycles;
        this.breaksEnabled = enableBreaks;
        this.longBreakInterval = longBreakInt;

        resetSessionStats();
    }

    // Configurazione semplificata (timer singolo)
    public void configureSingleTimer(int focusMins) {
        configureSession(focusMins, 0, 0, 1, false, 0);
    }

    private void resetSessionStats() {
        // Calcola durata totale stimata sessione
        if (breaksEnabled) {
            // Questo calcolo dipende da come "totalCycles" viene passato.
            // Assumiamo che totalCycles sia il numero TOTALE di pomodori da fare.

            // Logica semplificata per calcolo totale (ricopiata da HomeController)
            // Se totalCycles = N, abbiamo N blocchi di focus.
            // Le pause sono (N-1).
            // Ogni 'longBreakInterval' pausa è lunga, le altre corte.

            int focusSec = focusMinutes * 60;
            int shortSec = shortBreakMinutes * 60;
            int longSec = longBreakMinutes * 60;

            long tot = (long) totalCycles * focusSec;

            // Calcolo pause
            for (int i = 1; i < totalCycles; i++) {
                if (i % longBreakInterval == 0)
                    tot += longSec;
                else
                    tot += shortSec;
            }

            this.sessionTotalSeconds.set((int) tot);
        } else {
            this.sessionTotalSeconds.set(totalCycles * focusMinutes * 60);
        }

        this.sessionElapsedSeconds.set(0);
        this.completedCycles = 0;

        // Imposta stato iniziale
        setPhaseInternal(Phase.FOCUS, focusMinutes);
    }

    // ----------------- CONTROLLO TIMER -----------------

    public void start() {
        if (timeline != null)
            timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        timerState.set(TimerState.RUNNING);
    }

    public void stop() {
        if (timeline != null)
            timeline.stop();
        timerState.set(TimerState.IDLE);
    }

    public void reset() {
        stop();
        resetSessionStats(); // Ripristina all'inizio
    }

    private void tick() {
        remainingSeconds.set(Math.max(0, remainingSeconds.get() - 1));

        // Avanzamento sessione (solo se ha senso)
        if (sessionTotalSeconds.get() > 0 && sessionElapsedSeconds.get() < sessionTotalSeconds.get()) {
            sessionElapsedSeconds.set(sessionElapsedSeconds.get() + 1);
        }

        if (remainingSeconds.get() > 0)
            return;

        // FASE COMPLETATA
        onPhaseFinished();
    }

    private void onPhaseFinished() {
        if (phase.get() == Phase.FOCUS) {
            // Pomodoro completato!
            if (focusMinutes >= 25 && onPomodoroCompleted != null) {
                onPomodoroCompleted.run();
            }

            completedCycles++;

            if (!breaksEnabled) {
                stopAndResetUI();
                return;
            }

            boolean hasMoreCycles = completedCycles < totalCycles;
            if (!hasMoreCycles) {
                stopAndResetUI();
            } else {
                // Passa a pausa
                boolean longBreak = (completedCycles % longBreakInterval == 0);
                int bm = longBreak ? longBreakMinutes : shortBreakMinutes;
                setPhaseInternal(Phase.BREAK, bm);
            }

        } else { // ERA BREAK
            // Pausa finita, torna a focus
            boolean hasMoreCycles = completedCycles < totalCycles;
            if (!hasMoreCycles) {
                stopAndResetUI();
            } else {
                setPhaseInternal(Phase.FOCUS, focusMinutes);
            }
        }
    }

    private void stopAndResetUI() {
        stop();
        // Reimposta alla partenza per il prossimo giro
        completedCycles = 0;
        setPhaseInternal(Phase.FOCUS, focusMinutes);
    }

    private void setPhaseInternal(Phase p, int mins) {
        phase.set(p);
        totalSeconds.set(Math.max(1, mins) * 60);
        remainingSeconds.set(totalSeconds.get());
    }

    public void setOnPomodoroCompleted(Runnable callback) {
        this.onPomodoroCompleted = callback;
    }

    // ----------------- PROPERTIES EXPOSED -----------------

    public ReadOnlyObjectProperty<TimerState> timerStateProperty() {
        return timerState;
    }

    public TimerState getTimerState() {
        return timerState.get();
    }

    public ReadOnlyObjectProperty<Phase> phaseProperty() {
        return phase;
    }

    public Phase getPhase() {
        return phase.get();
    }

    public ReadOnlyIntegerProperty remainingSecondsProperty() {
        return remainingSeconds;
    }

    public int getRemainingSeconds() {
        return remainingSeconds.get();
    }

    public ReadOnlyIntegerProperty totalSecondsProperty() {
        return totalSeconds;
    }

    public int getTotalSeconds() {
        return totalSeconds.get();
    }

    public ReadOnlyIntegerProperty sessionElapsedSecondsProperty() {
        return sessionElapsedSeconds;
    }

    public ReadOnlyIntegerProperty sessionTotalSecondsProperty() {
        return sessionTotalSeconds;
    }

    public int getFocusMinutes() {
        return focusMinutes;
    }
}
