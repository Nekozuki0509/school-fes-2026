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
        for (Medias m : Medias.values()) {
            String src = Objects.requireNonNull(
                    Launcher.class.getResource(m.getPath())).toExternalForm();
            mediaSources.put(m, src);
            players.put(m, createPlayer(m, new Media(src)));
        }

        Launcher.init();
    }

    /**
     * Creates a MediaPlayer for the given type and Media object.
     * The player is intended to be long-lived and reused via stop → seek → play.
     *
     * All onEndOfMedia / onRepeat callbacks use Platform.runLater so they
     * never execute inside the native GStreamer callback stack.
     */
    private static MediaPlayer createPlayer(Medias type, Media media) {
        MediaPlayer player = new MediaPlayer(media);

        // Log errors but do NOT auto-recreate here.
        // Recovery is handled in safeSwitch when the player is actually needed.
        player.setOnError(() ->
                System.err.println("[MediaPlayer] ERROR on " + type + ": " + player.getError()));
        player.setOnStalled(() ->
                System.err.println("[MediaPlayer] STALLED on " + type + " – waiting for buffer…"));

        switch (type) {
            case GoOver, Start -> {
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setOnRepeat(() -> {
                    if (!requestedAction.isEmpty()) {
                        Platform.runLater(() -> {
                            Runnable r;
                            while ((r = requestedAction.poll()) != null) {
                                Runnable task = r;
                                new Thread(task).start();
                            }
                        });
                    }
                });
            }
            case Left, Right -> player.setOnEndOfMedia(() ->
                    Platform.runLater(() -> safeSwitch(lastAnswerCorrect ? Medias.Success : Medias.Fail)));
            case Success -> player.setOnEndOfMedia(() ->
                    Platform.runLater(() -> safeSwitch(lastWasLastProblem ? Medias.Start : Medias.GoOver)));
            case Fail -> player.setOnEndOfMedia(() ->
                    Platform.runLater(() -> safeSwitch(Medias.Start)));
            case Goal -> player.setOnEndOfMedia(() ->
                    Platform.runLater(() -> safeSwitch(Medias.GoOver)));
        }
        return player;
    }

    /**
     * Switches to the MediaPlayer for the given media type.
     *
     * Design: players are created once at startup and REUSED.  Switching is
     * simply stop(current) → seek(next, 0) → play(next).  No native resource
     * creation or disposal happens during gameplay, eliminating all GStreamer /
     * WMF resource-exhaustion freezes and ERROR_MEDIA_INVALID errors.
     *
     * If a player has entered the HALTED (unrecoverable error) state, it is
     * disposed and recreated as a one-time recovery — this will NOT loop
     * because the error handler does not auto-recreate.
     */
    public static void safeSwitch(Medias mediaType) {
        Runnable op = () -> {
            // ── 1. Stop the current player ──────────────────────────────
            MediaPlayer current = getInstance().getMediaView().getMediaPlayer();
            if (current != null) {
                current.stop();
            }

            // ── 2. Get the target player (reused, not newly created) ────
            MediaPlayer next = players.get(mediaType);

            // If the player is in an unrecoverable error state, recreate it
            // exactly once.  The new player's onError only logs, so this
            // cannot loop.
            if (next.getStatus() == MediaPlayer.Status.HALTED) {
                System.err.println("[MediaPlayer] Recovering HALTED player for " + mediaType);
                next.dispose();
                next = createPlayer(mediaType, new Media(mediaSources.get(mediaType)));
                players.put(mediaType, next);
            }

            // ── 3. Seek to start and play ───────────────────────────────
            getInstance().getMediaView().setMediaPlayer(next);
            next.seek(Duration.ZERO);
            next.play();
        };

        if (Platform.isFxApplicationThread()) {
            op.run();
        } else {
            CompletableFuture<Void> done = new CompletableFuture<>();
            Platform.runLater(() -> { op.run(); done.complete(null); });
            done.join();
        }
    }

}
