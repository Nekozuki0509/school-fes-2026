package com.github.nekozuki0509.schoolfes2026;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class Launcher {

    static final Problem lastProblem = new Problem("落ちたいですか？", "はい", "いいえ", -1);

    public static void main(String[] args) {
        Controller.setProblems(new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(Launcher.class.getResourceAsStream("problems.json"))), new TypeToken<Map<String, List<Problem>>>(){
        }.getType()));

        javafx.application.Application.launch(Application.class, args);
    }

    public static void init() {
        MediaPlayer start = Controller.getMediaPlayers().get(Medias.Start);
        Controller.getInstance().getMediaView().setMediaPlayer(start);
        start.play();

        Map<Integer, String> tmp = new HashMap<>();
        StringBuilder builder = new StringBuilder("pls type in course ");
        int i = 0;
        for (String course : Controller.getProblems().keySet()) {
            builder.append(i).append(": ").append(course).append(", ");
            tmp.put(i, course);
            i++;
        }
        game(tmp, builder.toString());
    }

    public static void game(Map<Integer, String> tmp, String courseAsk) {
        Controller.getConsole().ask(courseAsk)
                .thenAccept(ans -> {
                    System.out.printf("starting course: %s...%n", tmp.get(ans));
                    Controller.getRequestedAction().add(() -> {
                        Controller.getInstance().getMediaView().setMediaPlayer(Controller.getMediaPlayers().get(Medias.GoOver));
                        Controller.getMediaPlayers().get(Medias.GoOver).play();

                        AtomicBoolean last = new AtomicBoolean(false);
                        for (Problem problem : Controller.getProblems().get(tmp.get(ans))) {
                            ask(problem).thenAccept(last::set);

                            if (!last.get()) {
                                break;
                            }
                        }

                        if (last.get()) {
                            ask(lastProblem);
                        }

                        game(tmp, courseAsk);
                    });
                });
    }

    public static CompletableFuture<Boolean> ask(Problem problem) {
        Controller.setCurrentAns(problem.ans);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Controller.getInstance().getProblemImageView().setVisible(true);
        Controller.getInstance().getProblemTextArea().setText(problem.problem());
        Controller.getInstance().getProblemTextArea().setVisible(true);
        Controller.getInstance().getLeftImageView().setVisible(true);
        Controller.getInstance().getLeftTextArea().setText(problem.left);
        Controller.getInstance().getLeftTextArea().setVisible(true);
        Controller.getInstance().getRightImageView().setVisible(true);
        Controller.getInstance().getRightTextArea().setText(problem.right);
        Controller.getInstance().getRightTextArea().setVisible(true);
        Controller.getInstance().getProgressBar().setVisible(true);
        Controller.getInstance().getProgressBar().setProgress(1.0);
        Controller.setCountdownFinished(false);

        new Thread(() -> {
            double one = 1.0 / 15.0;
            boolean border = false;
            for (int j = 1; j <= 15; j++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Controller.getInstance().getProgressBar().setProgress(1.0-one*j);
                if (Controller.getSelectedAns() != -1) {
                    if (border) {
                        if (Controller.getSelectedAns() == 0) {
                            Controller.getInstance().getLeftImageView().setStyle("-fx-border-color: yellow; -fx-border-width: 5;");
                        } else {
                            Controller.getInstance().getRightImageView().setStyle("-fx-border-color: yellow; -fx-border-width: 5;");
                        }
                    } else {
                        if (Controller.getSelectedAns() == 0) {
                            Controller.getInstance().getLeftImageView().setStyle(Controller.getInstance().getLeftImageView().getStyle().replace("-fx-border-color: yellow; -fx-border-width: 5;", ""));
                        } else {
                            Controller.getInstance().getRightImageView().setStyle(Controller.getInstance().getRightImageView().getStyle().replace("-fx-border-color: yellow; -fx-border-width: 5;", ""));
                        }
                    }

                    border = !border;
                }
            }

            Controller.getInstance().getProgressBar().setProgress(0);
            Controller.setCountdownFinished(true);
        }).start();

        return Controller.getConsole().ask("pls type in the ans 0: left, 1: right")
                .thenApply(ans2 -> {
                    System.out.printf("ans accepted: %d%n", ans2);
                    Controller.setSelectedAns(ans2);

                    while (true) {
                        if (!Controller.isCountdownFinished()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            break;
                        }
                    }

                    Controller.getInstance().getProblemImageView().setVisible(false);
                    Controller.getInstance().getProblemTextArea().setVisible(false);
                    Controller.getInstance().getProblemTextArea().setText("");
                    Controller.getInstance().getLeftImageView().setVisible(false);
                    Controller.getInstance().getLeftTextArea().setVisible(false);
                    Controller.getInstance().getLeftTextArea().setText("");
                    Controller.getInstance().getRightImageView().setVisible(false);
                    Controller.getInstance().getRightTextArea().setVisible(false);
                    Controller.getInstance().getRightTextArea().setText("");
                    Controller.getInstance().getProgressBar().setVisible(false);
                    Controller.getInstance().getLeftImageView().setStyle(Controller.getInstance().getLeftImageView().getStyle().replace("-fx-border-color: yellow; -fx-border-width: 5;", ""));
                    Controller.getInstance().getRightImageView().setStyle(Controller.getInstance().getRightImageView().getStyle().replace("-fx-border-color: yellow; -fx-border-width: 5;", ""));

                    if (ans2 == 0) {
                        Controller.getRequestedAction().add(() -> {
                            MediaPlayer left = Controller.getMediaPlayers().get(Medias.Left);
                            Controller.getInstance().getMediaView().setMediaPlayer(left);
                            left.play();
                        });
                    } else {
                        Controller.getRequestedAction().add(() -> {
                            MediaPlayer right = Controller.getMediaPlayers().get(Medias.Right);
                            Controller.getInstance().getMediaView().setMediaPlayer(right);
                            right.play();
                        });
                    }

                    return ans2 == problem.ans || problem.ans == -1;
                });
    }

    public record Problem(String problem, String left, String right, int ans) {
    }
}

