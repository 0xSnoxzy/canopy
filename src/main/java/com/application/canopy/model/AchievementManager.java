package com.application.canopy.model;

import java.util.ArrayList;
import java.util.List;


 //Calcola lo stato di tutti gli achievement a partire dal GameState
 //Tutta la logica degli obiettivi sta qui
public class AchievementManager {

    private static final AchievementManager INSTANCE = new AchievementManager();


    private static final int MINUTI_PER_POMODORO = 25;

    private AchievementManager() {}

    public static AchievementManager getInstance() {
        return INSTANCE;
    }

    public List<AchievementGoal> evaluateAll(GameState gameState) {
        List<AchievementGoal> goals = new ArrayList<>();

        //progressione totale
        goals.add(germoglioDiDisciplina(gameState));
        goals.add(radiciSalde(gameState));
        goals.add(alberoGiovane(gameState));
        goals.add(querciaDellaCostanza(gameState));

        //orario del giorno
        goals.add(mattiniero(gameState));
        goals.add(notturnoSilenzioso(gameState));
        goals.add(forestaNotturna(gameState));
        goals.add(soleDiMezzogiorno(gameState));

        //streak e giornate intense
        goals.add(settimanaVerde(gameState));
        goals.add(fogliaNuova(gameState));
        goals.add(maratonetaDelFocus(gameState));

        //ritorno dopo pausa lunga + ore totali
        goals.add(rinascita(gameState));
        goals.add(alberoAntico(gameState));

        return goals;
    }

    //PROGRESSIONE TOTALE POMODORI

    private AchievementGoal germoglioDiDisciplina(GameState gs) {
        int total = gs.getTotalPomodoriGlobal();
        int current = Math.min(total, 1);

        return new AchievementGoal(
                AchievementId.GERMOGLIO_DI_DISCIPLINA,
                "Germoglio di Disciplina",
                "Completa il tuo primo pomodoro.",
                "Hai piantato il primo seme della tua foresta di focus completando un pomodoro. Ogni grande foresta inizia da qui.",
                current,
                1
        );
    }

    private AchievementGoal radiciSalde(GameState gs) {
        int total = gs.getTotalPomodoriGlobal();
        int current = Math.min(total, 10);

        return new AchievementGoal(
                AchievementId.RADICI_SALDE,
                "Radici Salde",
                "Completa 10 pomodori totali.",
                "Le tue abitudini stanno mettendo radici profonde. Hai completato 10 sessioni di focus: continua così!",
                current,
                10

        );
    }

    private AchievementGoal alberoGiovane(GameState gs) {
        int total = gs.getTotalPomodoriGlobal();
        int current = Math.min(total, 50);

        return new AchievementGoal(
                AchievementId.ALBERO_GIOVANE,
                "Albero Giovane",
                "Completa 50 pomodori totali.",
                "La tua pianta sta crescendo rigogliosa grazie alla tua costanza. Raggiunti 50 pomodori!",
                current,
                50
        );
    }

    private AchievementGoal querciaDellaCostanza(GameState gs) {
        int total = gs.getTotalPomodoriGlobal();
        int current = Math.min(total, 200);

        return new AchievementGoal(
                AchievementId.QUERCIA_DELLA_COSTANZA,
                "Quercia della Costanza",
                "Completa 200 pomodori totali.",
                "La tua disciplina è diventata una quercia solida. Hai completato 200 sessioni: un impegno straordinario.",
                current,
                200
        );
    }

    //ORARIO DELLA GIORNATA
    private AchievementGoal mattiniero(GameState gs) {
        boolean unlocked = gs.hasMorningPomodoroBefore9();
        int current = unlocked ? 1 : 0;

        return new AchievementGoal(
                AchievementId.MATTINIERO,
                "Mattiniero",
                "Completa un pomodoro prima delle 9:00.",
                "Ti sei messo al lavoro mentre il mondo si svegliava: ottimo inizio!",
                current,
                1
        );
    }

    private AchievementGoal notturnoSilenzioso(GameState gs) {
        boolean unlocked = gs.hasNightPomodoroAfter22();
        int current = unlocked ? 1 : 0;

        return new AchievementGoal(
                AchievementId.NOTTURNO_SILENZIOSO,
                "Notturno Silenzioso",
                "Completa un pomodoro dopo le 22:00.",
                "Anche di notte la tua concentrazione fiorisce. Non esagerare… ma ben fatto!",
                current,
                1
        );
    }

    private AchievementGoal forestaNotturna(GameState gs) {
        int count = gs.getTotalPomodoriAfter21();
        int current = Math.min(count, 5);

        return new AchievementGoal(
                AchievementId.FORESTA_NOTTURNA,
                "Foresta Notturna",
                "Completa 5 pomodori dopo le 21:00.",
                "Anche nel silenzio della notte la tua foresta si espande.",
                current,
                5
        );
    }

    private AchievementGoal soleDiMezzogiorno(GameState gs) {
        boolean unlocked = gs.hasCrossedNoonPomodoro();
        int current = unlocked ? 1 : 0;

        return new AchievementGoal(
                AchievementId.SOLE_DI_MEZZOGIORNO,
                "Sole di Mezzogiorno",
                "Completa un pomodoro a cavallo delle 12.",
                "Nel pieno della giornata hai trovato spazio per crescere ancora.",
                current,
                1
        );
    }

    //STREAK E GIORNI INTENSI
    private AchievementGoal settimanaVerde(GameState gs) {
        int bestStreak = gs.getGlobalBestStreak();
        int current = Math.min(bestStreak, 7);

        return new AchievementGoal(
                AchievementId.SETTIMANA_VERDE,
                "Settimana Verde",
                "Completa almeno un pomodoro al giorno per 7 giorni consecutivi.",
                "Una settimana intera di crescita costante: la tua foresta ti ringrazia.",
                current,
                7
        );
    }

    private AchievementGoal fogliaNuova(GameState gs) {
        int maxDay = gs.getMaxPomodoriInSingleDay();
        int current = Math.min(maxDay, 3);

        return new AchievementGoal(
                AchievementId.FOGLIA_NUOVA,
                "Foglia Nuova",
                "Completa 3 pomodori in un giorno.",
                "Una nuova foglia è spuntata: oggi hai nutrito bene la tua pianta.",
                current,
                3
        );
    }

    private AchievementGoal maratonetaDelFocus(GameState gs) {
        int maxDay = gs.getMaxPomodoriInSingleDay();
        int current = Math.min(maxDay, 8);

        return new AchievementGoal(
                AchievementId.MARATONETA_DEL_FOCUS,
                "Maratoneta del Focus",
                "Completa 8 pomodori in un singolo giorno.",
                "Una giornata di produttività intensa: hai mantenuto la concentrazione più a lungo di quanto facciano molti!",
                current,
                8
        );
    }

    // RITORNO DOPO PAUSA + ORE TOTALI
    private AchievementGoal rinascita(GameState gs) {
        boolean unlocked = gs.hasRinascitaUnlocked();
        int current = unlocked ? 1 : 0;

        return new AchievementGoal(
                AchievementId.RINASCITA,
                "Rinascita",
                "Completa un pomodoro dopo 7 giorni senza usar l’app.",
                "Sei tornato! Anche le piante che sembrano dormire possono rinascere: riprendiamo a crescere insieme.",
                current,
                1
        );
    }

    private AchievementGoal alberoAntico(GameState gs) {
        int totalPomodori = gs.getTotalPomodoriGlobal();
        int totalMinutes = totalPomodori * MINUTI_PER_POMODORO;
        int hours = totalMinutes / 60;
        int current = Math.min(hours, 50);

        return new AchievementGoal(
                AchievementId.ALBERO_ANTICO,
                "Albero Antico",
                "Raggiungi 50 ore totali di focus.",
                "La tua esperienza si misura in anelli di crescita: sei diventato un albero imponente.",
                current,
                50
        );
    }
}
