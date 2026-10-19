package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.git.MergeConflictDetails;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.ProjectIds;
import org.openl.studio.projects.model.merge.MergeConflictInfo;

class ProjectsMergeConflictsSessionHolderTest {

    private static final ProjectId PROJECT = ProjectIds.of("design", "Project");

    @Test
    void whatIsFoundAboutTheFilesOfAConflictIsKeptWithItUntilAConflictIsStoredAnew() {
        var holder = new ProjectsMergeConflictsSessionHolder();
        var conflict = conflict();
        holder.store(PROJECT, conflict);

        holder.textFilesOf(conflict).put("rules.xml", true);

        assertEquals(Map.of("rules.xml", true), holder.textFilesOf(conflict));
        holder.store(PROJECT, conflict);
        assertTrue(holder.textFilesOf(conflict).isEmpty());
    }

    @Test
    void conflictThatIsNotStoredKeepsNothingAboutItsFiles() {
        var holder = new ProjectsMergeConflictsSessionHolder();
        var stored = conflict();
        holder.store(PROJECT, stored);
        var other = conflict();

        holder.textFilesOf(other).put("rules.xml", true);

        assertTrue(holder.textFilesOf(other).isEmpty());
        assertTrue(holder.textFilesOf(stored).isEmpty());
        // Nor does any conflict while none is stored.
        assertTrue(new ProjectsMergeConflictsSessionHolder().textFilesOf(stored).isEmpty());
    }

    private static MergeConflictInfo conflict() {
        return MergeConflictInfo.builder()
                .details(MergeConflictDetails.builder().diffs(Map.of()).build())
                .project(mock(RulesProject.class))
                .build();
    }
}
