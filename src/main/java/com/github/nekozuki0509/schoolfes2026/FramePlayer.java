package com.github.nekozuki0509.schoolfes2026;

import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.media.AudioClip;



public class FramePlayer {

    private static final int FPS = 24;
    private static final long FRAME_NANOS = 1_000_000_000L / FPS;
    private static final int BUFFER_SIZE = 8; // 先読みフレーム数

    private final ImageView imageView;
    private AnimationTimer timer;

    private Medias current;
    private int frameIndex;
    private long lastFrameTime;

    private Runnable onEnd;
    private Runnable onRepeat;

    // 先読みバッファ
    private final BlockingQueue<Image> buffer = new LinkedBlockingQueue<>(BUFFER_SIZE);
    private ExecutorService loader;

    public FramePlayer(ImageView imageView) {
        this.imageView = imageView;
    }

    public void play(Medias media, Runnable onEnd, Runnable onRepeat) {
        stop();
        this.current = media;
        this.frameIndex = 0;
        this.lastFrameTime = 0;
        this.onEnd = onEnd;
        this.onRepeat = onRepeat;
        buffer.clear();

        loader = Executors.newSingleThreadExecutor();
        AtomicInteger loadIndex = new AtomicInteger(0);

        // バックグラウンドでフレームを先読み
        loader.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                int idx = loadIndex.getAndIncrement();
                if (idx >= media.getFrameCount()) break;
                String path = String.format(
                        "/com/github/nekozuki0509/schoolfes2026/textures/frames/%s/%s_%04d.jpg",
                        media.getFolderName(), media.getFolderName(), idx + 1);
                try {
                    Image img = new Image(
                            Objects.requireNonNull(FramePlayer.class.getResourceAsStream(path)),
                            1280, 720, false, false // リサイズしてデコード
                    );
                    buffer.put(img); // バッファが満杯なら待つ
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameTime == 0) {
                    lastFrameTime = now;
                    return;
                }
                if (now - lastFrameTime < FRAME_NANOS) return;
                lastFrameTime = now;

                Image img = buffer.poll(); // バッファから取得（なければスキップ）
                if (img == null) return;

                imageView.setImage(img);
                frameIndex++;

                if (frameIndex >= current.getFrameCount()) {
                    if (current.isLoop()) {
                        frameIndex = 0;
                        // ループ時は再度ローダーを起動
                        loadIndex.set(0);
                        buffer.clear();
                        loader.submit(() -> {
                            while (!Thread.currentThread().isInterrupted()) {
                                int idx = loadIndex.getAndIncrement();
                                if (idx >= current.getFrameCount()) break;
                                String p = String.format(
                                        "/com/github/nekozuki0509/schoolfes2026/textures/frames/%s/%s_%04d.jpg",
                                        current.getFolderName(), current.getFolderName(), idx + 1);
                                try {
                                    Image i = new Image(
                                            FramePlayer.class.getResourceAsStream(p),
                                            1280, 720, false, false);
                                    buffer.put(i);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        });
                        if (onRepeat != null) onRepeat.run();
                    } else {
                        stop();
                        if (media==Medias.Success) {
                            new Thread(() -> {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException ignored) {
                                }
                                String url = Objects.requireNonNull(
                                        FramePlayer.class.getResource(
                                                "/com/github/nekozuki0509/schoolfes2026/textures/music/tv_quiz_luxury_correct.mp3"
                                        )
                                ).toExternalForm();
                                AudioClip clip = new AudioClip(url);
                                clip.play();
                            }).start();
                        }
                        if (onEnd != null) onEnd.run();

                    }
                }
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (loader != null) {
            loader.shutdownNow();
            loader = null;
        }
        buffer.clear();
    }
}