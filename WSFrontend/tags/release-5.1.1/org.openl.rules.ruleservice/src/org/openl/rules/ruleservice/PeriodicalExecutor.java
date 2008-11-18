package org.openl.rules.ruleservice;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Runs a given {@link Runnable} every time an event happens. The main method {@link #execute()} runs infinite loop
 * and if another thread triggers the event via {@link #signal()} method calls <code>run()</code> on the object passed
 * in the constructor. If method {@link #signal()} was called several times since the last <code>Runnable</code>
 * execution (e.g. while the previous call to <code>run()</code> has not completed) that <code>Runnable</code>  will be
 * executed only once the next time. <br/>
 *
 * The object constructor calls {@link #signal()} so the <code>action.run()</code> is guaranteed to be called just after
 * calling {@link #execute()}.
 */
public class PeriodicalExecutor {
    /**
     * Action to execute periodically.
     */
    private final Runnable action;
    /**
     * A helper synchronization primitive. Note that <code>ArrayBlockingQueue</code> has capacity <i>1</i> so no more
     * than <i>1</i> object can be held by the queue.
     */
    private BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1);

    public PeriodicalExecutor(Runnable action) {
        this.action = action;
        signal();
    }

    /**
     * The calling thread begins infinite loop waiting for calls to  {@link #signal()} method from other threads to
     * execute associated action's <code>run</code> method.
     *
     * @throws InterruptedException if the thread was interrupted
     */
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
