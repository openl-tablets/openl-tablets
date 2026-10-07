package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.studio.projects.service.tables.TableTestProjects;

/** Opening a module again after what the session had compiled of it is gone. */
class ProjectModelResetTest {

    private static final String[][] ONE_TABLE = {{"Datatype Greeting", null}, {"String", "code"}};

    /**
     * A write to a module has it dropped and built again the next time it is opened. Between the two, the
     * session opened a module of another project, and the dependency manager was built anew for that project:
     * there is nothing of the written module left to drop, and opening it must go on all the same.
     */
    @Test
    void opensAWrittenModuleAgainAfterAnotherProjectReplacedWhatCompiledIt(@TempDir Path dir) throws Exception {
        var rates = moduleOf(TableTestProjects.writeProject(dir.resolve("Rates"), "Rates", "Rules", ONE_TABLE));
        var policies = moduleOf(TableTestProjects.writeProject(dir.resolve("Policies"), "Policies", "Rules", ONE_TABLE));
        var model = openedModel(rates);
        assertFalse(model.getCompiledOpenClass().hasErrors(), String.valueOf(model.getCompiledOpenClass().getAllMessages()));

        // Another project, outside the first one's workspace: its manager takes the place of the first one's.
        model.setModuleInfo(policies, ReloadType.SINGLE);
        awaitCompiled(model);
        assertEquals("Policies", model.getModuleInfo().getProject().getName());

        model.reset(ReloadType.SINGLE, rates);
        awaitCompiled(model);

        assertEquals("Rates", model.getModuleInfo().getProject().getName());
        assertFalse(model.getCompiledOpenClass().hasErrors(), "the written module is compiled afresh: "
                + model.getCompiledOpenClass().getAllMessages());
    }

    /**
     * The same, after the session let go of everything it had compiled - its project was closed by another
     * session, say - rather than compiling another project in its place.
     */
    @Test
    void opensAWrittenModuleAgainAfterTheSessionLetGoOfWhatCompiledIt(@TempDir Path dir) throws Exception {
        var rates = moduleOf(TableTestProjects.writeProject(dir.resolve("Rates"), "Rates", "Rules", ONE_TABLE));
        var model = openedModel(rates);

        model.clearModuleInfo();
        model.reset(ReloadType.SINGLE, rates);
        awaitCompiled(model);

        assertEquals("Rates", model.getModuleInfo().getProject().getName());
        assertFalse(model.getCompiledOpenClass().hasErrors(), "the written module is compiled afresh: "
                + model.getCompiledOpenClass().getAllMessages());
    }

    private static Module moduleOf(Path project) throws Exception {
        return ProjectResolver.getInstance().resolve(project).getModules().getFirst();
    }

    private static ProjectModel openedModel(Module module) throws Exception {
        var studio = mock(WebStudio.class);
        when(studio.getProjectResolver()).thenReturn(ProjectResolver.getInstance());
        var model = new ProjectModel(studio);
        model.setModuleInfo(module);
        awaitCompiled(model);
        return model;
    }

    private static void awaitCompiled(ProjectModel model) {
        try {
            model.getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
    }
}
