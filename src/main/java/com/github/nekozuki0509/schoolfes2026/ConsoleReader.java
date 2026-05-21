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
                    String input = scanner.nextLine().trim();
                    CompletableFuture<String> future = pendingQueue.poll();
                    if (future != null) {
                        future.complete(input);
                    } else {
                        System.out.println("no pending question, input ignored");
                    }
                }
            }
        }, "console-reader");
        t.setDaemon(true);
        t.start();
    }

    public CompletableFuture<Integer> ask(String prompt, int min, int max) {
        System.out.print(prompt + "\n>>>");
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingQueue.offer(future);

        return future.thenCompose(input -> {
            try {
                int ans = Integer.parseInt(input);
                if (ans < min || max < ans) {
                    System.out.println("invalid input. pls try again");
                    return ask(prompt, min, max);
                }

                return CompletableFuture.completedFuture(Integer.parseInt(input));
            } catch (NumberFormatException e) {
                System.out.println("invalid input. pls try again");
                return ask(prompt, min, max);
            }
        });
    }
}
