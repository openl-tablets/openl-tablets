package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class DebugSessionReaperTest {

    private static DebugSession sessionLastAccessedAt(long lastAccessMillis) {
        DebugSession session = mock(DebugSession.class);
        when(session.getLastAccessMillis()).thenReturn(lastAccessMillis);
        return session;
    }

    @Test
    void sweepTerminatesOnlySessionsIdlePastTheTimeout() {
        var reaper = new DebugSessionReaper(60_000, 60_000);
        var idle = sessionLastAccessedAt(0L);                            // far in the past
        var fresh = sessionLastAccessedAt(System.currentTimeMillis());   // just now
        reaper.register(idle);
        reaper.register(fresh);

        reaper.sweep();

        verify(idle).terminate();
        verify(fresh, never()).terminate();
        assertEquals(1, reaper.trackedCount(), "the idle session is dropped, the fresh one is kept");
    }

    @Test
    void unregisterStopsTracking() {
        var reaper = new DebugSessionReaper(60_000, 60_000);
        var session = sessionLastAccessedAt(System.currentTimeMillis());
        reaper.register(session);
        assertEquals(1, reaper.trackedCount());

        reaper.unregister(session);
        assertEquals(0, reaper.trackedCount());
    }

    @Test
    void stopTerminatesEveryRemainingSession() {
        var reaper = new DebugSessionReaper(60_000, 60_000);
        var session = sessionLastAccessedAt(System.currentTimeMillis());
        reaper.register(session);

        reaper.stop();

        verify(session).terminate();
        assertEquals(0, reaper.trackedCount());
    }
}
