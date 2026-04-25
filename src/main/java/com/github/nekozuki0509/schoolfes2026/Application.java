package com.github.nekozuki0509.schoolfes2026;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        stage.setTitle("School Festival 2026");
        stage.setScene(scene);
        stage.show();

        Controller ctrl = fxmlLoader.getController();
        makeTransparent(ctrl.getProblemTextArea(), false);
        makeTransparent(ctrl.getLeftTextArea(), true);
        makeTransparent(ctrl.getRightTextArea(), true);
    }


    private void makeTransparent(TextArea ta, boolean center) {
        String t = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-insets: 0;";
        if (center) t += "-fx-text-alignment: center;";
        ta.setStyle(t);
        for (Node node : ta.lookupAll("*")) {
            node.setStyle(t);
        }

        if (center) {
            for (Node node : ta.lookupAll(".text")) {
                if (node instanceof Text text) {
                    text.setTextAlignment(TextAlignment.CENTER);

                    Runnable updateY = () -> {
                        double textHeight = text.getBoundsInLocal().getHeight();
                        double areaHeight = ta.getHeight();
                        text.setTranslateY(Math.max(0, (areaHeight - textHeight) / 2.0));
                    };

                    text.boundsInLocalProperty().addListener((obs, o, n) -> updateY.run());
                    ta.heightProperty().addListener((obs, o, n) -> updateY.run());

                    updateY.run();
                }
            }

            Node content = ta.lookup(".content");
            if (content instanceof Region r) {
                r.setStyle(t + "-fx-alignment: center;");
            }
        }
    }
}
