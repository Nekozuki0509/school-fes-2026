package com.github.nekozuki0509.schoolfes2026;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
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

    static final Problem lastProblem = new Problem("落ちたいですか？", "はい", "YES", -1);

    public static void main(String[] args) {
        Controller.setProblems(new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(Launcher.class.getResourceAsStream("problems.json"))), new TypeToken<Map<String, List<Problem>>>() {
        }.getType()));

        javafx.application.Application.launch(Application.class, args);
    }

    public static void init() {
        Controller.safeSwitch(Medias.Start);

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
        Controller.getConsole().ask(courseAsk, 0, tmp.size() - 1)
                .thenAccept(ans -> {
                    System.out.printf("starting course: %s...%n", tmp.get(ans));
                    Controller.getRequestedAction().add(() -> {
                        Controller.safeSwitch(Medias.GoOver);

                        AtomicBoolean last = new AtomicBoolean(false);
                        for (Problem problem : Controller.getProblems().get(tmp.get(ans))) {
                            ask(problem).thenAccept(last::set).join();
                            if (!last.get()) break;
                        }

                        if (last.get()) {
                            Controller.getRequestedAction().add(() -> {
                                Controller.safeSwitch(Medias.Goal);
                                CompletableFuture<Void> goal_started = new CompletableFuture<>();

                                new Thread(() -> {
                                    goal_started.join();
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Controller.getRequestedAction().add(() -> {
                                        ask(lastProblem).join();
                                        game(tmp, courseAsk);
                                    });
                                }).start();

                                goal_started.complete(null);
                            });
                        } else {
                            game(tmp, courseAsk);
                        }
                    });
                });
    }

    public static CompletableFuture<Boolean> ask(Problem problem) {
        Controller.setCurrentAns(problem.ans);

        CompletableFuture<Void> uiReady = new CompletableFuture<>();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Platform.runLater(() -> {
            Controller.getInstance().getProblemImageView().setVisible(true);
            Controller.getInstance().getProblemLabel().setText(problem.problem);
            Controller.getInstance().getProblemLabel().setVisible(true);
            Controller.getInstance().getLeftImageView().setVisible(true);
            Controller.getInstance().getLeftLabel().setText(problem.left);
            Controller.getInstance().getLeftLabel().setVisible(true);
            Controller.getInstance().getRightImageView().setVisible(true);
            Controller.getInstance().getRightLabel().setText(problem.right);
            Controller.getInstance().getRightLabel().setVisible(true);
            Controller.getInstance().getProgressBar().setVisible(true);
            Controller.getInstance().getProgressBar().setProgress(1.0);
            Controller.getInstance().getProgressBar().setStyle("-fx-accent: green;");
            Controller.setCountdownFinished(false);
            uiReady.complete(null);
        });
        uiReady.join();

        new Thread(() -> {
            double one = 1.0 / 15.0;
            double now = 1.0;
            AtomicBoolean border = new AtomicBoolean(false);
            for (int j = 1; j <= 15; j++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                now -= one;
                double finalNow = now;
                int finalJ = j;
                Platform.runLater(() -> {
                    Controller.getInstance().getProgressBar().setProgress(finalNow);
                    if (finalJ == 8) {
                        Controller.getInstance().getProgressBar().setStyle("-fx-accent: yellow;");
                    } else if (finalJ == 13) {
                        Controller.getInstance().getProgressBar().setStyle("-fx-accent: red;");
                    }
                    if (Controller.getSelectedAns() != -1) {
                        ImageView target = Controller.getSelectedAns() == 0 ? Controller.getInstance().getRightImageView() : Controller.getInstance().getLeftImageView();
                        if (border.get()) {
                            target.setEffect(null);
                        } else {
                            DropShadow glow = new DropShadow();
                            glow.setColor(Color.YELLOW);
                            glow.setSpread(1.0);
                            glow.setRadius(15);
                            target.setEffect(glow);
                        }

                        border.set(!border.get());
                    }
                });
            }

            Platform.runLater(() -> Controller.getInstance().getProgressBar().setProgress(0));
            Controller.setCountdownFinished(true);
        }).start();

        return Controller.getConsole().ask("pls type in the ans 1: left, 0: right", 0, 1)
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

                    CompletableFuture<Void> uiCleared = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        Controller.getInstance().getProblemImageView().setVisible(false);
                        Controller.getInstance().getProblemLabel().setVisible(false);
                        Controller.getInstance().getProblemLabel().setText("");
                        Controller.getInstance().getLeftImageView().setVisible(false);
                        Controller.getInstance().getLeftLabel().setVisible(false);
                        Controller.getInstance().getLeftLabel().setText("");
                        Controller.getInstance().getRightImageView().setVisible(false);
                        Controller.getInstance().getRightLabel().setVisible(false);
                        Controller.getInstance().getRightLabel().setText("");
                        Controller.getInstance().getProgressBar().setVisible(false);
                        Controller.getInstance().getLeftImageView().setEffect(null);
                        Controller.getInstance().getRightImageView().setEffect(null);
                        uiCleared.complete(null);
                    });
                    uiCleared.join();

                    Controller.setLastAnswerCorrect(ans2 == Controller.getCurrentAns() || Controller.getCurrentAns() == -1);
                    Controller.setLastWasLastProblem(problem.ans == -1);
                    Controller.setSelectedAns(-1);
                    Controller.getRequestedAction().add(() -> Controller.safeSwitch(ans2==0?Medias.Right:Medias.Left));

                    return ans2 == problem.ans || problem.ans == -1;
                });
    }

    public record Problem(String problem, String left, String right, int ans) {
    }
}

