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
    private AutoShrinkLabel rightLabel;

    @Getter
    @Setter
    private static Map<String, List<Launcher.Problem>> problems;

    // URL strings for each media type (kept for error-recovery re-creation)
    private static final Map<Medias, String> mediaSources = new HashMap<>();

    // Persistent MediaPlayers — created once at startup, reused via stop/seek/play.
    // Never disposed during gameplay.  This avoids all GStreamer / WMF native
    // resource lifecycle issues that caused ERROR_MEDIA_INVALID and freezes.
    private static final Map<Medias, MediaPlayer> players = new HashMap<>();

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

    private static MediaView warmupView;

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

        // Create exactly ONE Media + MediaPlayer per type.
        // These are reused for the entire lifetime of the application.
        initPlayersSequentially(List.of(Medias.values()));
        mediaView.setCache(true);
        mediaView.setCacheHint(javafx.scene.CacheHint.SPEED);
    }

    private void initPlayersSequentially(List<Medias> remaining) {
        if (remaining.isEmpty()) {
            System.out.println("[Init] All players ready");
            initDone.complete(null);
            return;
        }

        Medias m = remaining.getFirst();
        List<Medias> rest = remaining.subList(1, remaining.size());

        String src = Objects.requireNonNull(
                Launcher.class.getResource(m.getPath())).toExternalForm();
        mediaSources.put(m, src);

        MediaPlayer player = createPlayer(m, new Media(src));
        players.put(m, player);

        player.setOnReady(() -> {
            if (players.get(m) != player) return;
            System.out.println("[Init] READY: " + m);
            player.setOnReady(null);
            player.setOnError(null);
            initPlayersSequentially(rest);
        });

        player.setOnError(() -> {
            if (players.get(m) != player) return;
            System.err.println("[Init] ERROR on " + m + ", retrying: " + player.getError());
            player.setOnReady(null);
            player.setOnError(null);
            player.dispose();
            // 同じmediumを先頭に戻して再試行
            List<Medias> retry = new java.util.ArrayList<>();
            retry.add(m);
            retry.addAll(rest);
            initPlayersSequentially(retry);
        });
    }

    private static MediaPlayer createPlayer(Medias type, Media media) {
        MediaPlayer player = new MediaPlayer(media);

        // Log errors but do NOT auto-recreate here.
        // Recovery is handled in safeSwitch when the player is actually needed.
        player.setOnError(() ->
                System.err.println("[MediaPlayer] ERROR on " + type + ": " + player.getError()));
        player.setOnStalled(() ->
                System.err.println("[MediaPlayer] STALLED on " + type + " – waiting for buffer…"));

        switch (type) {
            case GoOver, Select -> {
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setOnRepeat(() -> {
                    if (!requestedAction.isEmpty()) {
                        Platform.runLater(() -> {
                            Runnable r;
                            while ((r = requestedAction.poll()) != null) new Thread(r).start();
                        });
                    }
                });
            }
            case Left, Right -> player.setOnEndOfMedia(() ->
                    Platform.runLater(() -> safeSwitch(lastAnswerCorrect ? Medias.Success : Medias.Fail)));
            case Success -> player.setOnEndOfMedia(() ->
                    Platform.runLater(() -> safeSwitch(lastWasLastProblem ? Medias.Select : Medias.GoOver)));
            case Start -> player.setOnEndOfMedia(() ->
                    Platform.runLater(() -> safeSwitch(Medias.GoOver)));
        }
        return player;
    }

    public static void safeSwitch(Medias mediaType) {
        if (Platform.isFxApplicationThread()) {
            safeSwitchOp(mediaType, null);
        } else {
            CompletableFuture<Void> done = new CompletableFuture<>();
            Platform.runLater(() -> safeSwitchOp(mediaType, done));
            done.join();
        }
    }

    private static void safeSwitchOp(Medias mediaType, CompletableFuture<Void> done) {
        MediaPlayer next = players.get(mediaType);
        if (next == null) {
            if (done != null) done.complete(null);
            return;
        }
        MediaPlayer current = getInstance().getMediaView().getMediaPlayer();
        if (current != null && current != next) {
            current.stop();
        }
        getInstance().getMediaView().setMediaPlayer(next);
        next.seek(Duration.ZERO);
        next.play();
        if (done != null) done.complete(null);
    }
}