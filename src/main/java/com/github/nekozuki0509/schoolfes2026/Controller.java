package com.github.nekozuki0509.schoolfes2026;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Controller {

    @Getter
    private static Controller Instance;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @Getter
    @FXML
    private ImageView leftImageView;

    @Getter
    @FXML
    private AutoShrinkLabel leftLabel;

    @Getter
    @FXML
    private ImageView imageView;

    @FXML
    private AnchorPane pane;

    @Getter
    @FXML
    private ImageView problemImageView;

    @Getter
    @FXML
    private AutoShrinkLabel problemLabel;

    @Getter
    @FXML
    private ProgressBar progressBar;

    @Getter
    @FXML
    private ImageView rightImageView;

    @Getter
    @FXML
    private AutoShrinkLabel rightLabel;

    @Getter
    @Setter
    private static Map<String, List<Launcher.Problem>> problems;

    @Getter
    private static final Queue<Runnable> requestedAction = new ConcurrentLinkedDeque<>();

    @Getter
    private static final ConsoleReader console = new ConsoleReader();

    @Getter
    @Setter
    private static volatile int selectedAns = -1;

    @Getter
    @Setter
    private static volatile int currentAns = 0;

    @Getter
    @Setter
    private static volatile boolean countdownFinished = false;

    @Getter
    @Setter
    private static volatile boolean lastAnswerCorrect = false;

    @Getter
    @Setter
    private static volatile boolean lastWasLastProblem = false;

    @Getter
    private static final CompletableFuture<Void> initDone = new CompletableFuture<>();

    @Getter
    private static FramePlayer framePlayer;

    @FXML
    void initialize() {
        assert leftImageView != null : "fx:id=\"leftImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert leftLabel != null : "fx:id=\"leftLabel\" was not injected: check your FXML file 'main.fxml'.";
        assert imageView != null : "fx:id=\"imageView\" was not injected: check your FXML file 'main.fxml'.";
        assert pane != null : "fx:id=\"pane\" was not injected: check your FXML file 'main.fxml'.";
        assert problemImageView != null : "fx:id=\"problemImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert problemLabel != null : "fx:id=\"problemLabel\" was not injected: check your FXML file 'main.fxml'.";
        assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'main.fxml'.";
        assert rightImageView != null : "fx:id=\"rightImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert rightLabel != null : "fx:id=\"rightLabel\" was not injected: check your FXML file 'main.fxml'.";

        Instance = this;

        imageView.fitWidthProperty().bind(pane.widthProperty());
        imageView.fitHeightProperty().bind(pane.heightProperty());
        imageView.setPreserveRatio(false);

        problemImageView.fitWidthProperty().bind(pane.widthProperty().subtract(80));
        problemLabel.prefWidthProperty().bind(pane.widthProperty().subtract(80));

        leftImageView.fitWidthProperty().bind(pane.widthProperty().divide(2).subtract(60));
        leftLabel.prefWidthProperty().bind(pane.widthProperty().divide(2).subtract(60));

        rightImageView.fitWidthProperty().bind(pane.widthProperty().divide(2).subtract(60));
        rightLabel.prefWidthProperty().bind(pane.widthProperty().divide(2).subtract(60));

        leftImageView.fitHeightProperty().bind(pane.heightProperty().subtract(335));
        rightImageView.fitHeightProperty().bind(pane.heightProperty().subtract(335));

        imageView.setCache(true);
        imageView.setCacheHint(javafx.scene.CacheHint.SPEED);

        framePlayer = new FramePlayer(imageView);
        initDone.complete(null);
    }

    public static void safeSwitch(Medias mediaType) {
        Runnable op = () -> {
            Runnable onEnd = null;
            Runnable onRepeat = null;

            switch (mediaType) {
                case GoOver, Select -> onRepeat = () -> {
                    if (!requestedAction.isEmpty()) {
                        Runnable r;
                        while ((r = requestedAction.poll()) != null) {
                            new Thread(r).start();
                        }
                    }
                };
                case Left, Right -> onEnd = () ->
                        safeSwitch(lastAnswerCorrect ? Medias.Success : Medias.Fail);
                case Success -> onEnd = () ->
                        safeSwitch(lastWasLastProblem ? Medias.Select : Medias.GoOver);
                case Start -> onEnd = () -> safeSwitch(Medias.GoOver);
            }

            final Runnable finalOnEnd = onEnd;
            final Runnable finalOnRepeat = onRepeat;
            framePlayer.play(mediaType, finalOnEnd, finalOnRepeat);
        };

        if (Platform.isFxApplicationThread()) {
            op.run();
        } else {
            Platform.runLater(op);
        }
    }
}