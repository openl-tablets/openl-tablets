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
        var reaper = new DebugSessionReaper(60_000, 60_000, 16, 1_000);
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
    void registerEvictsTheLeastRecentlyAccessedWhenOverTheLimit() {
        var reaper = new DebugSessionReaper(60_000, 60_000, 2, 1_000);
        var oldest = sessionLastAccessedAt(1_000L);
        var middle = sessionLastAccessedAt(2_000L);
        var newest = sessionLastAccessedAt(3_000L);
        reaper.register(oldest);
        reaper.register(middle);
        reaper.register(newest); // exceeds the cap of 2

        verify(oldest).terminate();
        verify(middle, never()).terminate();
        verify(newest, never()).terminate();
        assertEquals(2, reaper.trackedCount(), "the two most-recently-accessed sessions are kept");
    }

    @Test
    void unregisterStopsTracking() {
        var reaper = new DebugSessionReaper(60_000, 60_000, 16, 1_000);
        var session = sessionLastAccessedAt(System.currentTimeMillis());
        reaper.register(session);
        assertEquals(1, reaper.trackedCount());

        reaper.unregister(session);
        assertEquals(0, reaper.trackedCount());
    }

    @Test
    void stopTerminatesEveryRemainingSession() {
        var reaper = new DebugSessionReaper(60_000, 60_000, 16, 1_000);
        var session = sessionLastAccessedAt(System.currentTimeMillis());
        reaper.register(session);

        reaper.stop();

        verify(session).terminate();
        assertEquals(0, reaper.trackedCount());
    }

    private static TraceTreeSnapshot treeLastReadAt(long lastAccessMillis, long nodeCount) {
        TraceTreeSnapshot tree = mock(TraceTreeSnapshot.class);
        when(tree.getLastAccessMillis()).thenReturn(lastAccessMillis);
        when(tree.getNodeCount()).thenReturn(nodeCount);
        return tree;
    }

    @Test
    void sweepReleasesOnlyTreesIdlePastTheTimeout() {
        var reaper = new DebugSessionReaper(60_000, 60_000, 16, 1_000);
        var idle = treeLastReadAt(0L, 10);
        var fresh = treeLastReadAt(System.currentTimeMillis(), 10);
        reaper.register(idle);
        reaper.register(fresh);

        reaper.sweep();

        verify(idle).release();
        verify(fresh, never()).release();
        assertEquals(1, reaper.trackedTreeCount());
    }

    @Test
    void registerReleasesTheLeastRecentlyReadTreesOverTheNodeBudget() {
        var reaper = new DebugSessionReaper(60_000, 60_000, 16, 1_000);
        var oldest = treeLastReadAt(1_000L, 400);
        var middle = treeLastReadAt(2_000L, 400);
        var newest = treeLastReadAt(3_000L, 400);
        reaper.register(oldest);
        reaper.register(middle);
        reaper.register(newest); // 1,200 nodes exceed the budget of 1,000

        verify(oldest).release();
        verify(middle, never()).release();
        verify(newest, never()).release();
        assertEquals(2, reaper.trackedTreeCount(), "the trees of 800 nodes fit the budget");
    }

    @Test
    void stopReleasesEveryTreeAndUnregisterStopsTracking() {
        var reaper = new DebugSessionReaper(60_000, 60_000, 16, 1_000);
        var kept = treeLastReadAt(System.currentTimeMillis(), 10);
        var dropped = treeLastReadAt(System.currentTimeMillis(), 10);
        reaper.register(kept);
        reaper.register(dropped);
        reaper.unregister(dropped);

        reaper.stop();

        verify(kept).release();
        verify(dropped, never()).release();
        assertEquals(0, reaper.trackedTreeCount());
    }
}
