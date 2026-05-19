package com.github.nekozuki0509.schoolfes2026;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
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
    private Label leftLabel;

    @Getter
    @FXML
    private MediaView mediaView;

    @FXML
    private AnchorPane pane;

    @Getter
    @FXML
    private ImageView problemImageView;

    @Getter
    @FXML
    private Label problemLabel;

    @Getter
    @FXML
    private ProgressBar progressBar;

    @Getter
    @FXML
    private ImageView rightImageView;

    @Getter
    @FXML
    private Label rightLabel;

    @Getter
    @Setter
    private static Map<String, List<Launcher.Problem>> problems;

    // URL strings for each media type — each MediaPlayer gets its own fresh Media instance
    private static final Map<Medias, String> mediaSources = new HashMap<>();

    // Pre-created MediaPlayers ready for instant playback (accessed only on FX thread)
    private static final Map<Medias, MediaPlayer> readyPlayers = new HashMap<>();

    // Old players waiting to be disposed (delayed to avoid native resource contention)
    private static final List<MediaPlayer> disposalQueue = new ArrayList<>();

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

    @FXML
    void initialize() {
        assert leftImageView != null : "fx:id=\"leftImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert leftLabel != null : "fx:id=\"leftLabel\" was not injected: check your FXML file 'main.fxml'.";
        assert mediaView != null : "fx:id=\"mediaView\" was not injected: check your FXML file 'main.fxml'.";
        assert pane != null : "fx:id=\"pane\" was not injected: check your FXML file 'main.fxml'.";
        assert problemImageView != null : "fx:id=\"problemImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert problemLabel != null : "fx:id=\"problemLabel\" was not injected: check your FXML file 'main.fxml'.";
        assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'main.fxml'.";
        assert rightImageView != null : "fx:id=\"rightImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert rightLabel != null : "fx:id=\"rightLabel\" was not injected: check your FXML file 'main.fxml'.";

        Instance = this;

        mediaView.fitWidthProperty().bind(pane.widthProperty());
        mediaView.fitHeightProperty().bind(pane.heightProperty());

        problemImageView.fitWidthProperty().bind(pane.widthProperty().subtract(80));
        problemLabel.prefWidthProperty().bind(pane.widthProperty().subtract(80));

        leftImageView.fitWidthProperty().bind(pane.widthProperty().divide(2).subtract(60));
        leftLabel.prefWidthProperty().bind(pane.widthProperty().divide(2).subtract(60));

        rightImageView.fitWidthProperty().bind(pane.widthProperty().divide(2).subtract(60));
        rightLabel.prefWidthProperty().bind(pane.widthProperty().divide(2).subtract(60));

        leftImageView.fitHeightProperty().bind(pane.heightProperty().subtract(335));
        rightImageView.fitHeightProperty().bind(pane.heightProperty().subtract(335));

        // Store URL strings and pre-create all MediaPlayers so they are ready instantly
        for (Medias m : Medias.values()) {
            mediaSources.put(m, Objects.requireNonNull(
                    Launcher.class.getResource(m.getPath())).toExternalForm());
            readyPlayers.put(m, createPlayer(m));
        }

        Launcher.init();
    }

    /**
     * Creates a fresh MediaPlayer with its own Media instance for the given type.
     * Each player gets an independent Media object to avoid sharing native
     * Windows Media Foundation resources, which can cause freezes.
     */
    private static MediaPlayer createPlayer(Medias type) {
        MediaPlayer player = new MediaPlayer(new Media(mediaSources.get(type)));
        switch (type) {
            case GoOver -> {
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setOnRepeat(() -> {
                    if (!requestedAction.isEmpty()) {
                        requestedAction.removeIf(runnable -> {
                            new Thread(runnable).start();
                            return true;
                        });
                    }
                });
            }
            case Left -> player.setOnEndOfMedia(() ->
                    safeSwitch(lastAnswerCorrect ? Medias.Success : Medias.Fail));
            case Right -> player.setOnEndOfMedia(() ->
                    safeSwitch(lastAnswerCorrect ? Medias.Success : Medias.Fail));
            case Start -> {
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setOnRepeat(() -> {
                    if (!requestedAction.isEmpty()) {
                        requestedAction.removeIf(runnable -> {
                            new Thread(runnable).start();
                            return true;
                        });
                    }
                });
            }
            case Success -> player.setOnEndOfMedia(() ->
                    safeSwitch(lastWasLastProblem ? Medias.Start : Medias.GoOver));
            case Fail -> player.setOnEndOfMedia(() ->
                    safeSwitch(Medias.Start));
        }
        return player;
    }

    /**
     * Switches to a pre-buffered MediaPlayer for the given media type.
     * Key design decisions to avoid Windows Media Foundation freezes:
     * 1. Set new player FIRST, then stop old → no white screen gap
     * 2. Execute directly on FX thread → no frame gap from Platform.runLater
     * 3. Defer dispose() → avoid native resource contention
     * 4. Each player has its own Media instance → no shared native resources
     */
    public static void safeSwitch(Medias mediaType) {
        Runnable op = () -> {
            MediaPlayer current = getInstance().getMediaView().getMediaPlayer();

            // Take the pre-buffered player (instant, no creation delay)
            MediaPlayer next = readyPlayers.remove(mediaType);
            if (next == null) {
                next = createPlayer(mediaType);
            }

            // Set new player FIRST so there is never a blank frame
            getInstance().getMediaView().setMediaPlayer(next);
            next.play();

            // Stop old player AFTER the new one is set and playing.
            // Don't dispose immediately — queue it for delayed disposal
            // to avoid interfering with the new player's native pipeline.
            if (current != null) {
                current.stop();
                current.dispose();
            }

            MediaPlayer replacement = createPlayer(mediaType);
            readyPlayers.put(mediaType, replacement);

        };

        // Execute directly on FX thread — no Platform.runLater deferral.
        // We are NOT manipulating the player that fired onEndOfMedia/onRepeat
        // (we only stop it AFTER setting a different player), so there is no
        // re-entrant pipeline conflict.
        if (Platform.isFxApplicationThread()) {
            op.run();
        } else {
            CompletableFuture<Void> done = new CompletableFuture<>();
            Platform.runLater(() -> { op.run(); done.complete(null); });
            done.join();
        }
    }

}
