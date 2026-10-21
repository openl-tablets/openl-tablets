package org.openl.studio.audit.service;

import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.locks.RenewableLockRegistry;
import org.springframework.stereotype.Component;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.common.utils.Periods;

/**
 * Removes the events of the audit log older than the retention period, {@code audit.retention}.
 *
 * <p>The period is written with its unit, such as {@code 90d}, {@code 2w}, {@code 6m} (months) or {@code 1y}, or in
 * ISO 8601, such as {@code P1Y6M}. A month and a year are those of the calendar, counted back in UTC.
 *
 * <p>The events are kept forever when the period is empty. Otherwise the old events are removed a few minutes after
 * the start and then once a day. In a cluster, one node at a time removes them. A period that cannot be read, or that
 * is not longer than zero, stops the start.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRetention {

    private static final String LOCK_KEY = "auditLogRetention";
    private static final long FIRST_RUN_DELAY_MINUTES = 5;
    private static final long RUN_INTERVAL_MINUTES = TimeUnit.DAYS.toMinutes(1);
    private static final int BATCH_SIZE = 1000;

    private final AuditService auditService;
    private final RenewableLockRegistry lockRegistry;

    @Value("${audit.retention:}")
    private final String retention;

    private @Nullable ScheduledExecutorService scheduler;

    @PostConstruct
    void start() {
        if (retentionPeriod() == null || !auditService.isEnabled()) {
            return;
        }
        var executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("audit-log-retention").factory());
        executor.scheduleWithFixedDelay(this::purgeQuietly, FIRST_RUN_DELAY_MINUTES, RUN_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
        scheduler = executor;
    }

    /**
     * Removes the events older than the retention period, unless another node is removing them, and records the
     * removal.
     *
     * <p>The events are removed in batches, each in a transaction of its own, so a large log is never removed in one
     * long transaction. The lock expires a few seconds after it was taken or last renewed, so each batch renews it:
     * another node never takes it while the removal runs.
     */
    void purge() {
        var period = retentionPeriod();
        var lock = lockRegistry.obtain(LOCK_KEY);
        if (period == null || !lock.tryLock()) {
            return;
        }
        try {
            var before = OffsetDateTime.now(ZoneOffset.UTC).minus(period).toInstant();
            var removed = 0;
            int batch;
            do {
                batch = auditService.removeBefore(before, BATCH_SIZE);
                removed += batch;
                lockRegistry.renewLock(LOCK_KEY);
            } while (batch == BATCH_SIZE);
            if (removed > 0) {
                auditService.recordEvent(AuditEvent.builder()
                        .action(AuditAction.AUDIT_LOG_PURGED)
                        .details(Map.of("before", before.toString(), "removed", removed))
                        .build());
            }
            log.info("Removed {} audit log events older than {}.", removed, period);
        } finally {
            lock.unlock();
        }
    }

    /**
     * The retention period, or nothing when the events are kept forever.
     *
     * @throws IllegalStateException when the period cannot be read or is not longer than zero
     */
    private @Nullable Period retentionPeriod() {
        try {
            return Periods.parse(retention);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("audit.retention = %s Leave it empty to keep the events forever."
                    .formatted(e.getMessage()), e);
        }
    }

    private void purgeQuietly() {
        try {
            purge();
        } catch (RuntimeException e) {
            log.warn("Failed to remove the old audit log events.", e);
        }
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
