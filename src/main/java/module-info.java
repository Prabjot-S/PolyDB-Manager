module com.example.polydb {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires jdk.compiler;


    opens com.example.polydb to javafx.fxml;
    exports com.example.polydb;
}