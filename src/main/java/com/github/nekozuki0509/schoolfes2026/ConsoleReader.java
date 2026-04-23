package com.github.nekozuki0509.schoolfes2026;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class ConsoleReader {

    private final LinkedBlockingDeque<CompletableFuture<String>> pendingQueue = new LinkedBlockingDeque<>();

    public ConsoleReader() {
        Thread t = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (!Thread.currentThread().isInterrupted()) {
                if (scanner.hasNextLine()) {
                    CompletableFuture<String> future = pendingQueue.poll();
                    if (future != null) {
                        future.complete(scanner.nextLine().trim());
                    }
                }
            }
        }, "console-reader");
        t.setDaemon(true);
        t.start();
    }

    public CompletableFuture<String> ask(String prompt) {
        System.out.print(prompt + "\n>>>");
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingQueue.offer(future);

        return future;
    }
}
