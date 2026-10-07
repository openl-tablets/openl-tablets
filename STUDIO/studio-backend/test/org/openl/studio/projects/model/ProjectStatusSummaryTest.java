package org.openl.studio.projects.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.ProjectStatus;

class ProjectStatusSummaryTest {

    @Test
    void mapsEachStatusToItsField() {
        var summary = ProjectStatusSummary.of(Map.of(
                ProjectStatus.LOCAL, 1L,
                ProjectStatus.VIEWING, 3L,
                ProjectStatus.VIEWING_VERSION, 4L,
                ProjectStatus.EDITING, 5L,
                ProjectStatus.CLOSED, 6L,
                ProjectStatus.DELETED, 7L));
        assertEquals(1L, summary.local());
        assertEquals(3L, summary.opened()); // VIEWING is reported as opened
        assertEquals(4L, summary.viewingVersion());
        assertEquals(5L, summary.editing());
        assertEquals(6L, summary.closed());
        assertEquals(7L, summary.deleted());
    }

    @Test
    void reportsMissingStatusesAsZero() {
        var summary = ProjectStatusSummary.of(Map.of(ProjectStatus.CLOSED, 2L));
        assertEquals(2L, summary.closed());
        assertEquals(0L, summary.local());
        assertEquals(0L, summary.opened());
        assertEquals(0L, summary.editing());
    }
}
