package org.openl.itest.core.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsyncExecutor {

    public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    private final ExecutorService executor;
    private final List<Wrapper> workers;
    private final int threads;

    public AsyncExecutor(int threads, Runnable command) {
        this.threads = threads;
        this.workers = Stream.generate(() -> new Wrapper(command))
                .limit(this.threads)
                .collect(Collectors.toList());
        this.executor = Executors.newFixedThreadPool(this.threads);
    }

    public void start() {
        workers.forEach(executor::submit);
        executor.shutdown();
    }

    public List<Throwable> stop() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        executor.shutdownNow();
        try {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            errors.add(e);
        }
        workers.stream()
                .map(Wrapper::getErrors)
                .forEach(errors::addAll);

        return Collections.unmodifiableList(errors);
    }

    private static final class Wrapper implements Runnable {

        private List<Throwable> errors = new ArrayList<>();
        private final Runnable delegate;
        private int invokedTimes = 0;

        Wrapper(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    delegate.run();
                } catch (Throwable error) {
                    errors.add(error);
                } finally {
                    invokedTimes++;
                }
            }
        }

        List<Throwable> getErrors() {
            return Collections.unmodifiableList(errors);
        }
    }

}
