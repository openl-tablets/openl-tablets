package org.openl.rules.repository.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openl.rules.repository.api.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For monitoring changes in a repository. If the difference is detected then a {@link Listener#onChange()} will be
 * called. Monitor uses {@link RevisionGetter#getRevision()} to retrieve the current change set revision.
 *
 * @author Yury Molchan
 */
public class ChangesMonitor implements Runnable {
    private final Logger log = LoggerFactory.getLogger(ChangesMonitor.class);
    private RevisionGetter getter;
    private int period;

    private ScheduledExecutorService scheduledPool;
    private ScheduledFuture<?> scheduled;
    private Listener callback;
    private Object lastChange;

    /**
     * Creates a monitor instance. In the end of the life cycle, {@link #release()} should be called to stop a
     * monitoring thread and release all instances of task and objects, those are held by this monitor.
     * 
     * @param getter for retrieving revisions of the monitored repository
     * @param period the comparison period of revisions in seconds
     */
    public ChangesMonitor(RevisionGetter getter, int period) {
        this.getter = getter;
        this.period = period;

    }

    public synchronized void setListener(Listener listener) {
        callback = listener;
        if (scheduled != null) {
            // stop the previous task
            scheduled.cancel(true);
        }
        if (listener == null) {
            // stop
            scheduled = null;
            lastChange = null;
        } else {
            if (scheduledPool == null) {
                // the first initialization of the thread pool.
                scheduledPool = Executors.newSingleThreadScheduledExecutor();
            }
            lastChange = getRevision();
            // run a new monitoring task
            scheduled = scheduledPool.scheduleWithFixedDelay(this, period, period, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        try {
            Object currentChange = getRevision();
            if (currentChange == null) {
                // Ignore unknown changes
                return;
            }
            if (currentChange.equals(lastChange)) {
                // Ignore no changes
                return;
            }
            lastChange = currentChange;

            fireOnChange();
        } catch (Throwable th) {
            log.warn("An exception has occurred during checking the repository", th);
        }
    }

    /**
     * Call onChange() method in the listener;
     */
    public void fireOnChange() {
        Listener listener = callback; // Copy for multi-thread
        try {
            if (listener != null) {
                listener.onChange();
            }
        } catch (Throwable th) {
            log.warn("An exception has occurred in onChange() method in '{}' listener", listener, th);
        }
    }

    /**
     * Stop the monitor.
     */
    public synchronized void release() {
        getter = null;
        if (scheduledPool != null) {
            scheduledPool.shutdownNow();
            scheduledPool = null;
        }
        if (scheduled != null) {
            scheduled.cancel(true);
            scheduled = null;
        }
    }

    private Object getRevision() {
        try {
            return getter.getRevision();
        } catch (Throwable th) {
            log.warn("An exception has occurred during retrieving the last change set from the repository", th);
            return null;
        }
    }
}
