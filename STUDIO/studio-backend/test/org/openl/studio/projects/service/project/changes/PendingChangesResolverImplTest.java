package org.openl.studio.projects.service.project.changes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalChanges;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.projects.model.project.status.ChangeType;
import org.openl.studio.projects.model.project.status.FileChange;

/**
 * The pending changes of a workspace project: the files changed since it was opened or saved.
 *
 * @author Yury Molchan
 */
class PendingChangesResolverImplTest {

    private final PendingChangesResolverImpl resolver = new PendingChangesResolverImpl();

    @Test
    void listsTheChangesMadeSinceTheProjectWasOpened() throws Exception {
        var design = mock(Repository.class);
        var project = project(design,
                new LocalChanges(List.of("/rules/New.xlsx"), List.of("/rules/Main.xlsx"), List.of("/Removed.txt")));

        var changes = resolver.resolve(project);

        assertEquals(List.of(new FileChange("DESIGN/rules/Example/rules/New.xlsx", ChangeType.ADDED),
                new FileChange("DESIGN/rules/Example/rules/Main.xlsx", ChangeType.MODIFIED),
                new FileChange("DESIGN/rules/Example/Removed.txt", ChangeType.DELETED)), changes.files());
        assertEquals(3, changes.total());
        // The repository holds what was opened, or what other programs wrote since: neither is a change made here.
        verify(design, never()).list(any());
    }

    @Test
    void answersNothingWhenNoFileChanged() throws Exception {
        var project = project(mock(Repository.class), new LocalChanges(List.of(), List.of(), List.of()));

        assertNull(resolver.resolve(project));
    }

    private static RulesProject project(Repository design, LocalChanges changes) throws Exception {
        var project = mock(RulesProject.class);
        when(project.isModified()).thenReturn(true);
        when(project.getRealPath()).thenReturn("DESIGN/rules/Example");
        when(project.getDesignRepository()).thenReturn(design);
        when(project.getLocalChanges()).thenReturn(changes);
        return project;
    }
}
