package org.openl.itest.core.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {

    private final ScheduledExecutorService scheduledExecutor;
    private final List<Throwable> errors;

    public TaskScheduler() {
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.errors = new ArrayList<>();
    }

    public void schedule(Runnable command, int delay, TimeUnit unit) {
        scheduledExecutor.schedule(wrap(command), delay, unit);
    }

    public List<Throwable> await() {
        scheduledExecutor.shutdown();

        try {
            scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            errors.add(e);
        }
        return Collections.unmodifiableList(errors);
    }

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
