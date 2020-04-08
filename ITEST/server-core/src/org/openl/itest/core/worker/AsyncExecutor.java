package org.openl.itest.core.worker;

import java.util.ArrayList;
import java.util.Collections;
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

    public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    private final ExecutorService executor;
    private final List<Wrapper> workers;
    private final int threads;

    /**
     * Executes a task as many time as thread number defined
     *
     * @param threads threads number to execute target task
     * @param command task to execute
     */
    public AsyncExecutor(int threads, Runnable command) {
        this.threads = threads;
        this.workers = Stream.generate(() -> new Wrapper(command)).limit(this.threads).collect(Collectors.toList());
        this.executor = Executors.newFixedThreadPool(this.threads);
    }

    /**
     * Executes tasks in its own thread
     *
     * @param commands tasks to execute
     */
    public AsyncExecutor(Runnable... commands) {
        this.threads = commands.length;
        this.workers = Stream.of(commands).map(Wrapper::new).collect(Collectors.toList());
        this.executor = Executors.newFixedThreadPool(this.threads);
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
     * @return all errors which were caught while commands execution
     */
    public List<Throwable> stop() {
        return stop(15, TimeUnit.SECONDS);
    }

    public List<Throwable> stop(int timeout, TimeUnit unit) {
        workers.forEach(Wrapper::stop);
        List<Throwable> errors = new ArrayList<>();
        executor.shutdownNow();
        try {
            executor.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            errors.add(e);
        }
        workers.stream().map(Wrapper::getErrors).forEach(errors::addAll);

        return Collections.unmodifiableList(errors);
    }

    /**
     * Task wrapper to all running of it until it is interrupted and catches all occurred errors
     * @author Vladyslav Pikus
     */
    private static final class Wrapper implements Runnable {

        private List<Throwable> errors = new ArrayList<>();
        private final Runnable delegate;
        private int invokedTimes = 0;
        private volatile boolean run = true;

        Wrapper(Runnable delegate) {
            this.delegate = delegate;
        }

        void stop() {
            this.run =  false;
        }

        @Override
        public void run() {
            while (run) {
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
