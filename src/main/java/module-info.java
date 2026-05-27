module com.github.nekozuki0509.schoolfes2026 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires static lombok;
    requires com.google.gson;
    requires java.desktop;


    opens com.github.nekozuki0509.schoolfes2026 to javafx.fxml;
    exports com.github.nekozuki0509.schoolfes2026;
}