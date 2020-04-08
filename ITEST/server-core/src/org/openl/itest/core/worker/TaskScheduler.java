package org.openl.itest.core.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The service allow to schedule running of commands after a given delay in one thread by default.<br/>
 * It wraps every command and catch any exception which was occurred during run.
 *
 * @author Vladyslav Pikus
 */
public class TaskScheduler {

    private final ScheduledExecutorService scheduledExecutor;
    private final List<Throwable> errors;

    /**
     * Initialize task scheduler with single thread executor
     */
    public TaskScheduler() {
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.errors = new ArrayList<>();
    }

    /**
     * Schedule a command with given delay
     *
     * @param command command to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     */
    public void schedule(Runnable command, int delay, TimeUnit unit) {
        scheduledExecutor.schedule(wrap(command), delay, unit);
    }

    /**
     * Shutdown thread executor to prevent of adding new commands and wait until all tasks are completed.<br/>
     * Interrupts all unfinished tasks after 10 seconds.
     *
     * @return all errors which were caught while commands execution
     */
    public List<Throwable> await() {
        return await(10, TimeUnit.SECONDS);
    }

    public List<Throwable> await(int timeout, TimeUnit unit) {
        scheduledExecutor.shutdown();

        try {
            boolean done = scheduledExecutor.awaitTermination(timeout, unit);
            if (!done) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            errors.add(e);
        }
        return Collections.unmodifiableList(errors);
    }

    /**
     * Wrap task for catching errors while execution
     *
     * @param command task to wrap
     * @return wrapped task
     */
    private Runnable wrap(Runnable command) {
        return () -> {
            try {
                command.run();
            } catch (Throwable e) {
                errors.add(e);
            }
        };
    }
}
