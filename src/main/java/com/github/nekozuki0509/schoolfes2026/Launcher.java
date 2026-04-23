package com.github.nekozuki0509.schoolfes2026;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public class Launcher {

    public static void main(String[] args) {
        Controller.setProblems(new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(Launcher.class.getResourceAsStream("problems.json"))), new TypeToken<Map<String, List<Problem>>>(){
        }.getType()));

        javafx.application.Application.launch(Application.class, args);
    }

    public static void init() {
        MediaPlayer start = Controller.getMediaPlayers().get(Medias.Start);
        Controller.getInstance().getMediaView().setMediaPlayer(start);
        start.play();

        System.out.println("invoked");

        Map<Integer, String> tmp = new HashMap<>();
        StringBuilder builder = new StringBuilder("pls type in course ");
        int i = 0;
        for (String course : Controller.getProblems().keySet()) {
            builder.append(i).append(": ").append(course).append(", ");
            tmp.put(i, course);
            i++;
        }

        Controller.getConsole().ask(builder.toString())
                .thenAccept(ans -> {
                    for (Problem problem : Controller.getProblems().get(tmp.get(Integer.parseInt(ans)))) {
                        Controller.getInstance().getProblemImageView().setVisible(true);
                        Controller.getInstance().getProblemTextArea().setText(problem.problem());
                        Controller.getInstance().getLeftImageView().setVisible(true);
                        Controller.getInstance().getLeftTextArea().setText(problem.left);
                        Controller.getInstance().getRightImageView().setVisible(true);
                        Controller.getInstance().getRightTextArea().setText(problem.right);
                        Controller.getInstance().getProgressBar().setVisible(true);
                        Controller.getInstance().getProgressBar().setProgress(100);
                    }
                });
    }

    public record Problem(String problem, String left, String right, int ans) {
    }
}

