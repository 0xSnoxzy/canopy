module com.application.canopy {
    requires javafx.controls;
    requires javafx.fxml;

    // consenti a FXMLLoader l’accesso via riflessione ai controller
    opens com.application.canopy.controller to javafx.fxml;

    // (opzionale ma tipico) esporta il package principale dell’app
    exports com.application.canopy;
}
