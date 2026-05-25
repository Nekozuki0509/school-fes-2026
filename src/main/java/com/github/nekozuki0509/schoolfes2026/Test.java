//package com.github.nekozuki0509.schoolfes2026;
//
//import javafx.scene.Scene;
//import javafx.scene.layout.StackPane;
//import javafx.scene.media.Media;
//import javafx.scene.media.MediaPlayer;
//import javafx.scene.media.MediaView;
//import javafx.stage.Stage;
//
//import java.io.File;
//import java.util.Objects;
//
//public class Test extends Application {
//    @Override
//    public void start(Stage stage) {
//        MediaPlayer player = new MediaPlayer(new Media(Objects.requireNonNull(Launcher.class.getResource(Medias.Fail.getPath())).toExternalForm()));
//        MediaView view = new MediaView(player);
//        stage.setScene(new Scene(new StackPane(view), 1280, 720));
//        stage.show();
//        player.play();
//    }
//
//    public static void main(String[] args) {
//        javafx.application.Application.launch(Test.class, args);
//    }
//}