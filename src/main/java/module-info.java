module com.application.canopy {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires java.sql;

    // consenti a FXMLLoader l’accesso via riflessione ai controller
    opens com.application.canopy.controller to javafx.fxml;

    opens com.application.canopy.model to javafx.fxml;

    // (opzionale ma tipico) esporta il package principale dell’app
    exports com.application.canopy;
}
