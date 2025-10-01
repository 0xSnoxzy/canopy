module com.example.canopy {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.canopy to javafx.fxml;
    exports com.example.canopy;
}