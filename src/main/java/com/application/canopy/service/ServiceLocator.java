package com.application.canopy.service;

import com.application.canopy.db.DatabaseManager;
import com.application.canopy.db.PlantActivityRepository;

import java.sql.SQLException;

/**
 * Service Locator per evitare che i controller debbano gestire
 * l'inizializzazione del database o dei repository (e i relativi try-catch).
 */
public class ServiceLocator {

    private static ServiceLocator instance;
    private PlantActivityRepository plantActivityRepository;

    private ServiceLocator() {
        // Init lazy o eager, qui facciamo lazy su richiesta o init esplicito
    }

    public static synchronized ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
    }

    /**
     * Restituisce il repository delle attività.
     * Gestisce internamente l'inizializzazione del DB se necessario.
     * Se il DB fallisce, restituisce null o lancia RuntimeException a seconda della
     * strategia.
     * Per semplicità, qui logghiamo e restituiamo null in caso di errore grave,
     * così l'app non crasha del tutto.
     */
    public PlantActivityRepository getPlantActivityRepository() {
        if (plantActivityRepository == null) {
            try {
                // Assicura che il DB sia connesso
                DatabaseManager.init();
                plantActivityRepository = new PlantActivityRepository(DatabaseManager.getConnection());
            } catch (SQLException e) {
                System.err.println("[ServiceLocator] Errore inizializzazione DB/Repository: " + e.getMessage());
                e.printStackTrace();
                // Possiamo decidere di ritornare null o un repository "dummy"
                return null;
            }
        }
        return plantActivityRepository;
    }
}
