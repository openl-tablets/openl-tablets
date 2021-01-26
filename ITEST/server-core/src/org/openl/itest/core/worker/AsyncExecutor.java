package org.openl.itest.core.worker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This service allows executing commands asynchronously during an infinite amount of time or until they are
 * interrupted<br/>
 * It wraps every command and catch any exception which was occurred during run.
 *
 * @author Vladyslav Pikus
 */
public class AsyncExecutor {

    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    private final ExecutorService executor;
    private final List<Wrapper> workers;
    private final int threads;

    /**
     * Executes a task as many time as twice cpu available
     *
     * @param command task to execute
     */
    public AsyncExecutor(Runnable command) {
        this(MAX_THREADS, command);
    }

    /**
     * Executes a task as many time as thread number defined
     *
     * @param threads threads number to execute target task
     * @param command task to execute
     */
    AsyncExecutor(int threads, Runnable command) {
        this.threads = threads;
        this.workers = Stream.generate(() -> new Wrapper(command)).limit(this.threads).collect(Collectors.toList());
        this.executor = Executors.newFixedThreadPool(this.threads);
    }

    private AsyncExecutor(Runnable... commands) {
        this.threads = commands.length;
        this.workers = Stream.of(commands).map(Wrapper::new).collect(Collectors.toList());
        this.executor = Executors.newFixedThreadPool(this.threads);
    }

    /**
     * Executes tasks in its own thread
     *
     * @param commands tasks to execute
     */
    public static AsyncExecutor start(Runnable... commands) {
        AsyncExecutor asyncExecutor = new AsyncExecutor(commands);
        asyncExecutor.start();
        return asyncExecutor;
    }
    /**
     * Start execution of all tasks
     */
    public void start() {
        workers.forEach(executor::submit);
        executor.shutdown();
    }

    /**
     * Interrupt execution of all run tasks and gather all errors which were caught while commands execution
     *
     * @return true if any errors occurs while commands execution
     */
    public boolean stop() {
        return stop(15, TimeUnit.SECONDS);
    }

    public boolean stop(int timeout, TimeUnit unit) {
        workers.forEach(Wrapper::stop);
        executor.shutdownNow();
        try {
            executor.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            e.printStackTrace(); // For debug purposes
            Thread.currentThread().interrupt();
            return true;
        }

        return workers.stream().anyMatch(Wrapper::hasError);
    }

    /**
     * Task wrapper to all running of it until it is interrupted and catches all occurred errors
     * 
     * @author Vladyslav Pikus
     */
    private static final class Wrapper implements Runnable {

        private volatile boolean error = false;
        private final Runnable delegate;
        private volatile boolean run = true;

        Wrapper(Runnable delegate) {
            this.delegate = delegate;
        }

        void stop() {
            this.run = false;
        }

        @Override
        public void run() {
            while (run && !error && !Thread.currentThread().isInterrupted()) {
                try {
                    delegate.run();
                } catch (Exception | AssertionError ex) {
                    error = true;
                    run = false;
                    ex.printStackTrace(); // For debug purposes
                }
            }
        }

        boolean hasError() {
            return error;
        }
    }

}
