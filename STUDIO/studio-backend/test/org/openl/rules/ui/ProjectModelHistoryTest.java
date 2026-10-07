package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.project.model.Module;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;

/** Covers the history the project keeps of the writes to the workbooks of its modules. */
class ProjectModelHistoryTest {

    @TempDir
    Path workspace;

    @Test
    void keepsTheWritesToAWorkbookOfAnotherModuleInTheHistoryOfThatModule() throws IOException {
        var state = mock(ProjectState.class);
        var model = new ProjectModel(studioOf(state));
        var listeners = new ArrayList<XlsWorkbookListener>();
        var source = Files.writeString(workspace.resolve("Rates.xlsx"), "as the module was");
        var workbook = workbookOf(source, listeners);
        var module = mock(Module.class);
        when(module.getRulesRootPath()).thenReturn("rules/Rates.xlsx");

        model.initProjectHistory(tableIn(workbook), module);
        model.initProjectHistory(tableIn(workbook), module);
        assertEquals(1, listeners.size(), "A workbook is listened to once");

        var listener = listeners.getFirst();
        listener.beforeSave(workbook);
        Files.writeString(source, "as the theme wrote it");
        listener.afterSave(workbook);

        // The module keeps the version the write replaced and the one it wrote, and the project is modified.
        try (var versions = Files.list(workspace.resolve(".history/Pricing/rules/Rates.xlsx"))) {
            assertEquals(2, versions.count());
        }
        verify(state).notifyModified();
    }

    /** A session with a workspace of its own, the project Pricing open in it. */
    private WebStudio studioOf(ProjectState state) {
        var repository = mock(LocalRepository.class);
        when(repository.getProjectState(anyString())).thenReturn(state);
        var local = mock(LocalWorkspace.class);
        when(local.getLocation()).thenReturn(workspace.toFile());
        when(local.getRepository(any())).thenReturn(repository);
        var userWorkspace = mock(UserWorkspace.class);
        when(userWorkspace.getLocalWorkspace()).thenReturn(local);
        var project = mock(RulesProject.class);
        when(project.getFolderPath()).thenReturn("Pricing");
        var studio = mock(WebStudio.class);
        when(studio.getUserWorkspace()).thenReturn(userWorkspace);
        when(studio.getCurrentProject()).thenReturn(project);
        return studio;
    }

    /** A workbook written to the given file, keeping the listeners added to it in the given list. */
    private static XlsWorkbookSourceCodeModule workbookOf(Path source, List<XlsWorkbookListener> listeners) {
        var workbook = mock(XlsWorkbookSourceCodeModule.class);
        when(workbook.getListeners()).thenReturn(listeners);
        doAnswer(invocation -> listeners.add(invocation.getArgument(0))).when(workbook).addListener(any());
        when(workbook.getSourceFile()).thenReturn(source.toFile());
        return workbook;
    }

    /** A table compiled from a sheet of the workbook. */
    private static TableSyntaxNode tableIn(XlsWorkbookSourceCodeModule workbook) {
        var sheet = mock(XlsSheetSourceCodeModule.class);
        when(sheet.getWorkbookSource()).thenReturn(workbook);
        var table = mock(TableSyntaxNode.class);
        when(table.getXlsSheetSourceCodeModule()).thenReturn(sheet);
        return table;
    }
}
