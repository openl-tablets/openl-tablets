package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.integration.support.locks.RenewableLockRegistry;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;

class AuditRetentionTest {

    private AuditService auditService;
    private RenewableLockRegistry lockRegistry;
    private Lock lock;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        lockRegistry = mock(RenewableLockRegistry.class);
        lock = mock(Lock.class);
        when(lockRegistry.obtain(any())).thenReturn(lock);
        when(auditService.isEnabled()).thenReturn(true);
    }

    @Test
    void removesTheEventsOlderThanTheRetentionPeriodInBatchesAndRecordsTheRemoval() {
        when(lock.tryLock()).thenReturn(true);
        when(auditService.removeBefore(any(), eq(1000))).thenReturn(1000, 1000, 7);

        new AuditRetention(auditService, lockRegistry, "30d").purge();

        var before = ArgumentCaptor.forClass(Instant.class);
        verify(auditService, times(3)).removeBefore(before.capture(), eq(1000));
        assertAbout(Instant.now().minus(Duration.ofDays(30)), before.getValue());
        assertEquals(1, before.getAllValues().stream().distinct().count(), "Every batch removes before one moment");
        verify(lockRegistry, times(3)).renewLock("auditLogRetention");
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).recordEvent(event.capture());
        assertEquals(AuditAction.AUDIT_LOG_PURGED, event.getValue().action());
        assertEquals(Map.of("before", before.getValue().toString(), "removed", 2007), event.getValue().details());
        verify(lock).unlock();
    }

    @ParameterizedTest
    @CsvSource({"90d, P90D", "2w, P14D", "6m, P6M", "1y, P1Y", "1Y6m, P1Y6M", "P1Y6M, P1Y6M", "p2y, P2Y"})
    void readsAPeriodWithItsUnitAndCountsItBackInTheCalendarOfUtc(String retention, String period) {
        when(lock.tryLock()).thenReturn(true);

        new AuditRetention(auditService, lockRegistry, retention).purge();

        var before = ArgumentCaptor.forClass(Instant.class);
        verify(auditService).removeBefore(before.capture(), eq(1000));
        assertAbout(OffsetDateTime.now(ZoneOffset.UTC).minus(Period.parse(period)).toInstant(), before.getValue());
    }

    @Test
    void recordsNoRemovalWhenNothingIsOld() {
        when(lock.tryLock()).thenReturn(true);

        new AuditRetention(auditService, lockRegistry, "30d").purge();

        verify(auditService).removeBefore(any(), eq(1000));
        verify(auditService, never()).recordEvent(any());
        verify(lock).unlock();
    }

    @Test
    void leavesTheRemovalToTheNodeThatHoldsTheLock() {
        when(lock.tryLock()).thenReturn(false);

        new AuditRetention(auditService, lockRegistry, "30d").purge();

        verify(auditService, never()).removeBefore(any(), anyInt());
        verify(lock, never()).unlock();
    }

    @Test
    void keepsTheEventsForeverWithoutAPeriod() {
        when(lock.tryLock()).thenReturn(true);

        new AuditRetention(auditService, lockRegistry, "").purge();
        new AuditRetention(auditService, lockRegistry, "  ").purge();

        verify(auditService, never()).removeBefore(any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0d", "P0D", "-1d", "1y-13m", "365", "12h", "a year"})
    void refusesToStartWithAPeriodThatCannotBeReadOrIsNotLongerThanZero(String retention) {
        var refused = new AuditRetention(auditService, lockRegistry, retention);

        var error = assertThrows(IllegalStateException.class, refused::start);

        assertTrue(error.getMessage().startsWith("audit.retention = '" + retention + "'"), error.getMessage());
        verify(auditService, never()).removeBefore(any(), anyInt());
    }

    @Test
    void startsAndStopsTheScheduleOnlyWhenEventsExpire() {
        var limited = new AuditRetention(auditService, lockRegistry, "30d");
        limited.start();
        limited.stop();

        var unlimited = new AuditRetention(auditService, lockRegistry, "");
        unlimited.start();
        unlimited.stop();

        when(auditService.isEnabled()).thenReturn(false);
        var singleUser = new AuditRetention(auditService, lockRegistry, "30d");
        singleUser.start();
        singleUser.stop();

        verify(auditService, never()).removeBefore(any(), anyInt());
    }

    /** Asserts the moment is the expected one, give or take the minute the test may take. */
    private static void assertAbout(Instant expected, Instant actual) {
        assertTrue(Duration.between(actual, expected).abs().toSeconds() < 60, () -> actual + " is not " + expected);
    }
}
