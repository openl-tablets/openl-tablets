package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.studio.projects.service.tables.TableTestProjects;

/** Whether what the session compiled still answers for the workbooks it was compiled from. */
class ProjectModelWrittenSinceCompiledTest {

    private static final String[][] HELLO = {{"Method String hello(String name)"}, {"Return \"Hi, \" + name;"}};
    private static final String[][] BYE = {{"Method String bye(String name)"}, {"Return \"Bye, \" + name;"}};

    @Test
    void a_write_waits_until_its_module_is_compiled_again(@TempDir Path dir) throws Exception {
        TableTestProjects.writeProject(dir, "Rules", "Rules", HELLO);
        var module = moduleOf(dir, "Rules");
        var model = opened(module, false);
        assertFalse(model.isWrittenSinceCompiled(), "a module just compiled answers for its workbook");

        saveWorkbookOf(model);

        assertTrue(model.isWrittenSinceCompiled(), "what was compiled is the workbook as it stood before the write");
        model.reset(ReloadType.SINGLE, module);
        awaitCompiled(model);
        assertFalse(model.isWrittenSinceCompiled(), "the module compiled again reads the workbook as written");
    }

    @Test
    void verify_on_another_module_compiles_the_write_again(@TempDir Path dir) throws Exception {
        TableTestProjects.writeProject(dir, "Rules", "Rules", HELLO);
        TableTestProjects.writeProject(dir, "Other", "Other", BYE);
        var model = opened(moduleOf(dir, "Rules"), false);
        saveWorkbookOf(model);

        // Another module is compiled with the one written to, as it stood before the write.
        var other = moduleOf(dir, "Other");
        model.setModuleInfo(other);
        awaitCompiled(model);
        assertTrue(model.isManualCompileNeeded(), "the module the reader went on to waits for Verify too");

        // Verify compiles every module again, whichever module it is asked for.
        model.reset(ReloadType.RELOAD, other);
        awaitCompiled(model);
        assertFalse(model.isManualCompileNeeded());
    }

    @Test
    void the_reader_is_asked_to_verify_only_where_compilation_is_manual(@TempDir Path dir) throws Exception {
        TableTestProjects.writeProject(dir, "Rules", "Rules", HELLO);
        var model = opened(moduleOf(dir, "Rules"), true);

        saveWorkbookOf(model);

        // Compiling again is what the next read of the module does by itself.
        assertFalse(model.isManualCompileNeeded());
        assertTrue(model.isWrittenSinceCompiled());
    }

    @Test
    void a_file_changed_by_another_program_shows_until_its_module_is_compiled_again(@TempDir Path dir)
            throws Exception {
        TableTestProjects.writeProject(dir, "Rules", "Rules", HELLO);
        var module = moduleOf(dir, "Rules");
        var model = opened(module, true);
        saveWorkbookOf(model);
        assertFalse(model.isChangedOnDisk(), "a save of the session itself is no change");

        // Another program writes the workbook: a project of its own is written over it.
        var other = Files.createDirectory(dir.resolve("other"));
        TableTestProjects.writeProject(other, "Rules", "Rules", BYE);
        Files.copy(other.resolve("Rules.xlsx"), dir.resolve("Rules.xlsx"), StandardCopyOption.REPLACE_EXISTING);

        assertTrue(model.isChangedOnDisk());
        model.reset(ReloadType.RELOAD, module);
        awaitCompiled(model);
        assertFalse(model.isChangedOnDisk(), "the module compiled again reads the file as the other program left it");
    }

    @Test
    void a_workbook_added_to_the_project_shows_until_the_project_is_compiled_again(@TempDir Path dir)
            throws Exception {
        TableTestProjects.writeProject(dir, "Rules", "Rules", HELLO);
        var module = moduleOf(dir, "Rules");
        var model = opened(module, true);
        model.checkProjectFiles();
        assertFalse(model.isChangedOnDisk(), "the files are the ones compiled");

        // Another program puts a workbook into the folder the modules are found in.
        TableTestProjects.writeProject(dir, "Other", "Other", BYE);
        assertFalse(model.isChangedOnDisk(), "nothing is noted until the repository tells of a change");
        model.checkProjectFiles();

        assertTrue(model.isChangedOnDisk(), "the project declares a module that was not compiled");
        model.reset(ReloadType.RELOAD, moduleOf(dir, "Rules"));
        awaitCompiled(model);
        assertFalse(model.isChangedOnDisk(), "the project compiled again holds the new module");
    }

    @Test
    void a_changed_or_broken_descriptor_shows(@TempDir Path dir) throws Exception {
        TableTestProjects.writeProject(dir, "Rules", "Rules", HELLO);
        var model = opened(moduleOf(dir, "Rules"), true);

        Files.writeString(dir.resolve("rules.xml"), """
                <project>
                    <name>Renamed</name>
                </project>
                """);
        model.checkProjectFiles();
        assertTrue(model.isChangedOnDisk(), "a project descriptor written since is a change");

        Files.writeString(dir.resolve("rules.xml"), "<project>");
        model.checkProjectFiles();
        assertTrue(model.isChangedOnDisk(), "a descriptor that cannot be read is a change too");
    }

    private static Module moduleOf(Path dir, String name) throws Exception {
        return ProjectResolver.getInstance()
                .resolve(dir)
                .getModules()
                .stream()
                .filter(module -> name.equals(module.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static ProjectModel opened(Module module, boolean autoCompile) throws Exception {
        var studio = mock(WebStudio.class);
        when(studio.isAutoCompile()).thenReturn(autoCompile);
        when(studio.getProjectResolver()).thenReturn(ProjectResolver.getInstance());
        var model = new ProjectModel(studio);
        model.setModuleInfo(module);
        awaitCompiled(model);
        return model;
    }

    /** Saves the workbook of the open module, as a write of one of its tables does. */
    private static void saveWorkbookOf(ProjectModel model) throws IOException {
        model.getXlsModuleNode().getWorkbookSyntaxNodes()[0].getWorkbookSourceCodeModule().save();
    }

    private static void awaitCompiled(ProjectModel model) {
        try {
            model.getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
    }
}
