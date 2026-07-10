package org.openl.rules.workspace.lw.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;

class FolderHelperTest {

    @Test
    void historyFolderIsOutsideTheProjectFolder() {
        Module module = mock(Module.class);
        when(module.getRulesRootPath()).thenReturn("Bank Rating .xlsx");

        RulesProject project = mock(RulesProject.class);
        when(project.getFolderPath()).thenReturn("Example 1");

        assertEquals(".history/Example 1/Bank Rating .xlsx", FolderHelper.resolveHistoryFolder(project, module));

        assertEquals(Path.of("/workspaces/jdoe/.history/Example 1/Bank Rating .xlsx"),
                FolderHelper.resolveHistoryFolder(Path.of("/workspaces/jdoe/Example 1"), module));
    }

}
