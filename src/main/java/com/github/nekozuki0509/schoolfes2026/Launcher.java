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
        while (true) {
            Controller.safeSwitch(Medias.Select);

            int ans = Controller.getConsole().ask(courseAsk, 0, tmp.size() - 1).join();

            System.out.printf("starting course: %s...%n", tmp.get(ans));

            CompletableFuture<Void> go_over_started = new CompletableFuture<>();
            Controller.getRequestedAction().add(() -> {
                Controller.safeSwitch(Medias.Start);
                go_over_started.complete(null);
            });
            go_over_started.join();

            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean last = false;
            for (Problem problem : Controller.getProblems().get(tmp.get(ans))) {
                CompletableFuture<Boolean> ask_com = ask(problem).join();
                last = ask_com.join();

                if (!last) break;

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (last) {
                CompletableFuture<Void> go_over_fin = new CompletableFuture<>();
                Controller.getRequestedAction().add(() -> {
                    Controller.safeSwitch(Medias.Goal);
                    go_over_fin.complete(null);
                });
                go_over_fin.join();

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ask(lastProblem).join();
                Controller.safeSwitch(Medias.Fail);
            }


            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static CompletableFuture<CompletableFuture<Boolean>> ask(Problem problem) {
        Controller.setCurrentAns(problem.ans);

        CompletableFuture<Void> uiReady = new CompletableFuture<>();

        Platform.runLater(() -> {
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
                    e.printStackTrace();
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
                                e.printStackTrace();
                            }
                        } else {
                            break;
                        }
                    }

                    CompletableFuture<Void> uiCleared = new CompletableFuture<>();
                    Platform.runLater(() -> {
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

                    Controller.setLastAnswerCorrect(ans2 == Controller.getCurrentAns());
                    Controller.setLastWasLastProblem(problem.ans == -1);
                    Controller.setSelectedAns(-1);

                    CompletableFuture<Boolean> diverse_fin = new CompletableFuture<>();
                    if (problem.ans != -1) {
                        CompletableFuture<Void> go_over_fin = new CompletableFuture<>();
                        Controller.getRequestedAction().add(() -> {
                            Controller.safeSwitch(ans2 == 0 ? Medias.Right : Medias.Left);
                            go_over_fin.complete(null);
                        });
                        go_over_fin.join();

                        diverse_fin.complete(ans2 == problem.ans);
                    }

                    return diverse_fin;
                });
    }

    public record Problem(String problem, String left, String right, int ans) {
    }
}