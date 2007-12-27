package org.openl.rules.ruleservice;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class PeriodicalExecutor {
    private final Runnable action;
    private BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1);

    public PeriodicalExecutor(Runnable action) {
        this.action = action;
        signal();
    }

    public void execute() throws InterruptedException {
        while (true) {
            queue.take();
            action.run();
        }
    }

    public void signal() {
        queue.offer(new Object());
    }
}
