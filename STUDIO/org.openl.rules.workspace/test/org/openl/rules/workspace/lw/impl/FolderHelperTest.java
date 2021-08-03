package org.openl.rules.workspace.lw.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;

public class FolderHelperTest {

    @Test
    public void testResolveHistoryFolder() {
        Module module = mock(Module.class);
        PathEntry pathEntry = mock(PathEntry.class);
        when(module.getRulesRootPath()).thenReturn(pathEntry);
        when(pathEntry.getPath()).thenReturn("Bank Rating .xlsx");

        assertEquals(".history/Bank Rating .xlsx", FolderHelper.resolveHistoryFolder(module));

        RulesProject project = mock(RulesProject.class);
        when(project.getFolderPath()).thenReturn("Example 1");

        assertEquals("Example 1/.history/Bank Rating .xlsx", FolderHelper.resolveHistoryFolder(project, module));
    }

}
