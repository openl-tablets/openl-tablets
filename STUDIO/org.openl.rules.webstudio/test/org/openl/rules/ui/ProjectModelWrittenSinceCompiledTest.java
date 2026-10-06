package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
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
