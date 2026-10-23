package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.project.model.Module;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;

/** Covers the history the project keeps of the writes to the workbooks of its modules. */
class ProjectModelHistoryTest {

    @TempDir
    Path workspace;

    private Environment previousEnvironment;

    @BeforeEach
    void useSettings() {
        // A write to the history reads from the settings how many versions to keep: no limit here.
        previousEnvironment = Props.getEnvironment();
        Props.setEnvironment(new MockEnvironment());
    }

    @AfterEach
    void restoreSettings() {
        Props.setEnvironment(previousEnvironment);
    }

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

    @Test
    void keepsTheVersionAnotherProgramWroteBeforeTheNextWrite() throws IOException {
        var model = new ProjectModel(studioOf(mock(ProjectState.class)));
        var listeners = new ArrayList<XlsWorkbookListener>();
        var source = Files.writeString(workspace.resolve("Rates.xlsx"), "as the module was");
        var workbook = workbookOf(source, listeners);
        var module = mock(Module.class);
        when(module.getRulesRootPath()).thenReturn("rules/Rates.xlsx");
        model.initProjectHistory(tableIn(workbook), module);
        var listener = listeners.getFirst();
        listener.beforeSave(workbook);
        Files.writeString(source, "as the session wrote it");
        listener.afterSave(workbook);

        Files.writeString(source, "as Excel wrote it");
        listener.beforeSave(workbook);
        Files.writeString(source, "as the session wrote it again");
        listener.afterSave(workbook);

        // The first version, each write of the session, and between them the version Excel wrote.
        var history = workspace.resolve(".history/Pricing/rules/Rates.xlsx");
        try (var versions = Files.list(history)) {
            var contents = versions.sorted().map(ProjectModelHistoryTest::read).toList();
            assertEquals(4, contents.size());
            assertTrue(contents.contains("as Excel wrote it"));
        }
    }

    /** A workbook as the history took it is not compared with the history again: the history holds it. */
    @Test
    void comparesNoWorkbookThatStayedAsTheHistoryTookIt() throws IOException {
        var model = new ProjectModel(studioOf(mock(ProjectState.class)));
        var listeners = new ArrayList<XlsWorkbookListener>();
        var source = Files.writeString(workspace.resolve("Rates.xlsx"), "as the module was");
        var workbook = workbookOf(source, listeners);
        var module = mock(Module.class);
        when(module.getRulesRootPath()).thenReturn("rules/Rates.xlsx");
        model.initProjectHistory(tableIn(workbook), module);
        var listener = listeners.getFirst();
        listener.beforeSave(workbook);
        Files.writeString(source, "as the session wrote it");
        listener.afterSave(workbook);
        var history = workspace.resolve(".history/Pricing/rules/Rates.xlsx");
        // The copy the history holds is told apart from the file here, so that a comparison would keep the file.
        try (var versions = Files.list(history)) {
            var current = versions.filter(version -> version.getFileName().toString().endsWith("_current"))
                    .findFirst()
                    .orElseThrow();
            Files.writeString(current, "another copy");
        }

        listener.beforeSave(workbook);

        try (var versions = Files.list(history)) {
            assertEquals(2, versions.count(), "the first version and the write of the session");
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        when(project.getLocalRepository()).thenReturn(repository);
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
