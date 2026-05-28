package com.github.nekozuki0509.schoolfes2026;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) {
        BGMPlayer bgm = new BGMPlayer();
        bgm.load(Application.class.getResource("textures/music/maou_game_dangeon19.wav"));
        bgm.setVolume(70f);
        bgm.play();
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main.fxml"));
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), 1280, 720);
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.setTitle("School Festival 2026");
        stage.setScene(scene);
        new Thread(() -> {
            Controller.getInitDone().join();
            Launcher.init();
        }).start();

        stage.show();
    }
}
