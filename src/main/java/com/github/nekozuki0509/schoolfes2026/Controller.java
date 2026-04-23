package com.github.nekozuki0509.schoolfes2026;

import java.net.URL;
import java.util.*;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import lombok.Getter;
import lombok.Setter;

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
    private TextArea leftTextArea;

    @Getter
    @FXML
    private MediaView mediaView;

    @FXML
    private Pane pane;

    @Getter
    @FXML
    private ImageView problemImageView;

    @Getter
    @FXML
    private TextArea problemTextArea;

    @Getter
    @FXML
    private ProgressBar progressBar;

    @Getter
    @FXML
    private ImageView rightImageView;

    @Getter
    @FXML
    private TextArea rightTextArea;

    @Getter
    @Setter
    private static Map<String, List<Launcher.Problem>> problems;

    @Getter
    private static final Map<Medias, MediaPlayer> mediaPlayers = new HashMap<>();

    private static final Queue<MediaPlayer> requestedPlayers = new ArrayDeque<>();

    @Getter
    private static final ConsoleReader console = new ConsoleReader();

    @FXML
    void initialize() {
        assert leftImageView != null : "fx:id=\"leftImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert leftTextArea != null : "fx:id=\"leftTextArea\" was not injected: check your FXML file 'main.fxml'.";
        assert mediaView != null : "fx:id=\"mediaView\" was not injected: check your FXML file 'main.fxml'.";
        assert pane != null : "fx:id=\"pane\" was not injected: check your FXML file 'main.fxml'.";
        assert problemImageView != null : "fx:id=\"problemImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert problemTextArea != null : "fx:id=\"problemTextArea\" was not injected: check your FXML file 'main.fxml'.";
        assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'main.fxml'.";
        assert rightImageView != null : "fx:id=\"rightImageView\" was not injected: check your FXML file 'main.fxml'.";
        assert rightTextArea != null : "fx:id=\"rightTextArea\" was not injected: check your FXML file 'main.fxml'.";

        Instance = this;

        MediaPlayer goOver = new MediaPlayer(new Media(Objects.requireNonNull(Launcher.class.getResource(Medias.GoOver.getPath())).toExternalForm()));
        MediaPlayer left = new MediaPlayer(new Media(Objects.requireNonNull(Launcher.class.getResource(Medias.Left.getPath())).toExternalForm()));
        MediaPlayer right = new MediaPlayer(new Media(Objects.requireNonNull(Launcher.class.getResource(Medias.Right.getPath())).toExternalForm()));
        MediaPlayer start = new MediaPlayer(new Media(Objects.requireNonNull(Launcher.class.getResource(Medias.Start.getPath())).toExternalForm()));
        MediaPlayer success = new MediaPlayer(new Media(Objects.requireNonNull(Launcher.class.getResource(Medias.Success.getPath())).toExternalForm()));
        MediaPlayer fail = new MediaPlayer(new Media(Objects.requireNonNull(Launcher.class.getResource(Medias.Fail.getPath())).toExternalForm()));

        goOver.setCycleCount(MediaPlayer.INDEFINITE);
        goOver.setOnEndOfMedia(() -> {
            if (!requestedPlayers.isEmpty()) {
                MediaPlayer nextPlayer = requestedPlayers.poll();
                mediaView.setMediaPlayer(nextPlayer);
                nextPlayer.play();
            }
        });

        left.setOnEndOfMedia(() -> {
            mediaView.setMediaPlayer(mediaPlayers.get(Medias.GoOver));
            mediaPlayers.get(Medias.GoOver).play();
        });

        right.setOnEndOfMedia(() -> {
            mediaView.setMediaPlayer(mediaPlayers.get(Medias.GoOver));
            mediaPlayers.get(Medias.GoOver).play();
        });

        start.setCycleCount(MediaPlayer.INDEFINITE);
        start.setOnEndOfMedia(() -> {
            if (!requestedPlayers.isEmpty()) {
                MediaPlayer nextPlayer = requestedPlayers.poll();
                mediaView.setMediaPlayer(nextPlayer);
                nextPlayer.play();
            }
        });

        success.setOnEndOfMedia(() -> {
            throw new IllegalStateException("todo");
        });

        fail.setOnEndOfMedia(() -> {
            mediaView.setMediaPlayer(mediaPlayers.get(Medias.Start));
            mediaPlayers.get(Medias.Start).play();
        });

        Controller.getMediaPlayers().put(Medias.GoOver, goOver);
        Controller.getMediaPlayers().put(Medias.Left, left);
        Controller.getMediaPlayers().put(Medias.Right, right);
        Controller.getMediaPlayers().put(Medias.Start, start);
        Controller.getMediaPlayers().put(Medias.Success, success);
        Controller.getMediaPlayers().put(Medias.Fail, fail);

        Launcher.init();
    }

}
