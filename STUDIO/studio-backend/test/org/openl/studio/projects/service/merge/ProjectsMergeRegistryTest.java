package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.ProjectIds;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeResult;

/**
 * A client runs one merge at a time: the same merge asked for again is the one running, another one is refused.
 */
class ProjectsMergeRegistryTest {

    private static final ProjectId PROJECT = ProjectIds.of("design", "RateCalculator");

    private final ProjectsMergeRegistry registry = new ProjectsMergeRegistry();

    private static StartedMerge merge(String otherBranch, MergeOpMode mode) {
        return StartedMerge.builder()
                .projectId(PROJECT)
                .currentBranch("dev")
                .otherBranch(otherBranch)
                .mode(mode)
                .userName("admin")
                .editor(mock(StartedMerge.Editor.class))
                .build();
    }

    @Test
    void knowsNoMergeBeforeOneIsTaken() {
        assertFalse(registry.isRunning(PROJECT, "master", MergeOpMode.SEND));
        assertEquals(Optional.empty(), registry.current());
    }

    @Test
    void tellsTheSameMergeRunning() {
        var merge = merge("master", MergeOpMode.SEND);

        registry.set(merge);

        assertTrue(registry.isRunning(PROJECT, "master", MergeOpMode.SEND));
        assertSame(merge, registry.find(PROJECT).orElseThrow());
    }

    @Test
    void refusesAnotherMergeWhileOneRuns() {
        registry.set(merge("master", MergeOpMode.SEND));
        var otherProject = ProjectIds.of("design", "Other");

        assertThrows(ConflictException.class, () -> registry.isRunning(PROJECT, "master", MergeOpMode.RECEIVE));
        assertThrows(ConflictException.class, () -> registry.isRunning(PROJECT, "release", MergeOpMode.SEND));
        assertThrows(ConflictException.class, () -> registry.isRunning(otherProject, "master", MergeOpMode.SEND));
        assertEquals(Optional.empty(), registry.find(otherProject));
    }

    @Test
    void letsGoOfAnEndedMergeOfTheProjectOnly() {
        var merge = merge("master", MergeOpMode.SEND);
        registry.set(merge);

        registry.forget(ProjectIds.of("design", "Other"));
        registry.forget(PROJECT);
        assertSame(merge, registry.current().orElseThrow());

        merge.task().complete(MergeResult.builder().build());
        registry.forget(PROJECT);
        assertEquals(Optional.empty(), registry.current());
    }

    @Test
    void keepsAnEndedMergeUntilTheNextOneIsTaken() {
        var ended = merge("master", MergeOpMode.SEND);
        registry.set(ended);
        ended.task().complete(MergeResult.builder().build());

        // The same merge again, once ended, is a merge of its own: there may be new changes to merge.
        assertFalse(registry.isRunning(PROJECT, "master", MergeOpMode.SEND));
        assertSame(ended, registry.current().orElseThrow());

        var next = merge("release", MergeOpMode.RECEIVE);
        registry.set(next);

        assertSame(next, registry.current().orElseThrow());
    }
}
