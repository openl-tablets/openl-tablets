package org.openl.rules.webstudio.web.diff;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class DiffManager implements AutoCloseable {
    private final Map<String, Comparison> diffControllers = new HashMap<>();
    private final int cleanUpPeriod;
    private ScheduledExecutorService scheduledPool;
    private ScheduledFuture<?> scheduled;

    public DiffManager(@Value("${diff-manager.inactive-cleanup-period}") int cleanUpPeriod) {
        this.cleanUpPeriod = cleanUpPeriod;
    }

    void add(String id, ShowDiffController diff) {
        synchronized (diffControllers) {
            diffControllers.put(id, new Comparison(diff, System.currentTimeMillis()));

            if (scheduledPool == null) {
                activateCleanup();
            }
        }
    }

    ShowDiffController get(String id) {
        synchronized (diffControllers) {
            Comparison comparison = diffControllers.get(id);
            if (comparison == null) {
                return null;
            }
            comparison.setAccessTime(System.currentTimeMillis());
            return comparison.getController();
        }
    }

    void scheduleForRemove(String id) {
        // Comparison can be resurrected in 10 seconds. If not resurrected in this time, remove it.
        int timeToResurrect = 10;

        // 100ms is inaccuracy because of jsf specifics: jsf invokes get() after the page was closed.
        int inaccuracy = 100;
        long removeTime = System.currentTimeMillis() + inaccuracy;
        scheduledPool.schedule(() -> {
            synchronized (diffControllers) {
                Comparison comparison = diffControllers.get(id);
                if (comparison != null && comparison.getAccessTime() <= removeTime) {
                    // Not requested comparison during waiting to remove. Can clean up.
                    diffControllers.remove(id);
                    comparison.getController().close();
                }
            }
        }, timeToResurrect, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        if (scheduledPool != null) {
            scheduledPool.shutdownNow();
        }
        if (scheduled != null) {
            scheduled.cancel(true);
            scheduled = null;
        }
        if (scheduledPool != null) {
            try {
                scheduledPool.awaitTermination(cleanUpPeriod, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            scheduledPool = null;
        }

        synchronized (diffControllers) {
            for (Comparison comparison : diffControllers.values()) {
                comparison.getController().close();
            }
            diffControllers.clear();
        }
    }

    private void activateCleanup() {
        scheduledPool = Executors.newSingleThreadScheduledExecutor();
        scheduled = scheduledPool
            .scheduleWithFixedDelay(this::cleanUpInactive, cleanUpPeriod, cleanUpPeriod, TimeUnit.SECONDS);
    }

    private void cleanUpInactive() {
        synchronized (diffControllers) {
            long currentTime = System.currentTimeMillis();
            Iterator<Comparison> iterator = diffControllers.values().iterator();
            while (iterator.hasNext()) {
                Comparison comparison = iterator.next();
                if ((currentTime - comparison.getAccessTime()) / 1000 >= cleanUpPeriod) {
                    iterator.remove();
                    comparison.getController().close();
                }
            }
        }
    }

    private static class Comparison {
        private final ShowDiffController controller;
        private long accessTime;

        private Comparison(ShowDiffController controller, long accessTime) {
            this.controller = controller;
            this.accessTime = accessTime;
        }

        ShowDiffController getController() {
            return controller;
        }

        long getAccessTime() {
            return accessTime;
        }

        void setAccessTime(long accessTime) {
            this.accessTime = accessTime;
        }
    }
}
