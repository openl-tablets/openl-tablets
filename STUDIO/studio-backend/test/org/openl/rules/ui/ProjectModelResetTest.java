package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.studio.projects.service.tables.TableTestProjects;

/** Opening a module again after what the session had compiled of it is gone. */
class ProjectModelResetTest {

    private static final String[][] ONE_TABLE = {{"Datatype Greeting", null}, {"String", "code"}};

    private final List<ProjectModel> models = new ArrayList<>();

    /** Lets go of what each model compiled, its compilation thread included. */
    @AfterEach
    void destroyModels() {
        models.forEach(ProjectModel::destroy);
    }

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

    /**
     * A module is compiled again from its workbook while the compilation of its project is still on its way. That
     * compilation hands its result over under the lock of the model, which the reset holds: the reset must not
     * wait for it, and once it ends it must not change the model the reset built anew.
     *
     * <p>What the replaced compilation built is released before the reset goes on, its class loaders included: the
     * manager built next may share the projects, and it must not find their jars released under it later.
     */
    @Test
    void resetsAtOnceWhileTheProjectIsStillCompiling(@TempDir Path dir) throws Exception {
        var rates = moduleOf(TableTestProjects.writeProject(dir.resolve("Rates"), "Rates", "Rules", ONE_TABLE));
        var model = modelOf();
        Compiling replaced;
        ClassLoader replacedLoader;
        synchronized (model) {
            replaced = compiling(model, rates);
            replacedLoader = replaced.manager().getExternalJarsClassLoader(rates.getProject());
            assertTimeout(Duration.ofSeconds(10), () -> model.reset(ReloadType.RELOAD, rates),
                    "the reset does not wait for the compilation it replaces");
            assertNotSame(replacedLoader, replaced.manager().getExternalJarsClassLoader(rates.getProject()),
                    "what the replaced compilation built is released once the reset returns");
        }
        awaitCompiled(model);

        assertNotSame(replaced.manager(), model.getWebStudioWorkspaceDependencyManager());
        assertTrue(replaced.compilation().future().isCancelled(), "the replaced compilation ends without a result");
        assertFalse(model.isCompilationInProgress(), "the compilation that replaced it has finished");
        assertFalse(model.getCompiledOpenClass().hasErrors(),
                String.valueOf(model.getCompiledOpenClass().getAllMessages()));
    }

    /**
     * The same reset to a module the project no longer has. Nothing is compiled in place of what was let go of,
     * and the project is not left compiling.
     */
    @Test
    void resetToNoModuleLeavesNothingCompiling(@TempDir Path dir) throws Exception {
        var rates = moduleOf(TableTestProjects.writeProject(dir.resolve("Rates"), "Rates", "Rules", ONE_TABLE));
        var model = modelOf();
        Compiling replaced;
        synchronized (model) {
            replaced = compiling(model, rates);
            assertTimeout(Duration.ofSeconds(10), () -> model.reset(ReloadType.RELOAD, null),
                    "the reset does not wait for the compilation it lets go of");
        }

        assertFalse(replaced.manager().isActive(), "the manager compiles nothing more");
        assertTrue(replaced.compilation().future().isCancelled(), "the compilation ends without a result");
        assertFalse(model.isCompilationInProgress(), "nothing is compiling once the compilation is let go of");
    }

    /**
     * The session lets go of the module while its project is still compiling, before the files of the project
     * are changed or removed. It does not wait for the compilation, and nothing is left compiling.
     */
    @Test
    void clearsTheModuleAtOnceWhileTheProjectIsStillCompiling(@TempDir Path dir) throws Exception {
        var rates = moduleOf(TableTestProjects.writeProject(dir.resolve("Rates"), "Rates", "Rules", ONE_TABLE));
        var model = modelOf();
        Compiling replaced;
        synchronized (model) {
            replaced = compiling(model, rates);
            assertTimeout(Duration.ofSeconds(10), model::clearModuleInfo,
                    "clearing does not wait for the compilation on its way");
        }

        assertFalse(replaced.manager().isActive(), "the manager compiles nothing more");
        assertTrue(replaced.compilation().future().isCancelled(), "the compilation ends without a result");
        assertFalse(model.isCompilationInProgress(), "nothing is compiling once the module is cleared");
    }

    /** The compilation of a project on its way, and the dependency manager it runs on. */
    private record Compiling(RegisteredCompilation compilation, WebStudioWorkspaceRelatedDependencyManager manager) {
    }

    /**
     * Opens the module, which compiles its project in the background. Called while holding the lock of the model,
     * the compilation cannot hand its result over until the lock is let go of.
     */
    private static Compiling compiling(ProjectModel model, Module module) throws Exception {
        model.setModuleInfo(module);
        return new Compiling(model.getCurrentCompilation(), model.getWebStudioWorkspaceDependencyManager());
    }

    private static Module moduleOf(Path project) throws Exception {
        return ProjectResolver.getInstance().resolve(project).getModules().getFirst();
    }

    private ProjectModel openedModel(Module module) throws Exception {
        var model = modelOf();
        model.setModuleInfo(module);
        awaitCompiled(model);
        return model;
    }

    private ProjectModel modelOf() {
        var studio = mock(WebStudio.class);
        when(studio.getProjectResolver()).thenReturn(ProjectResolver.getInstance());
        var model = new ProjectModel(studio);
        models.add(model);
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
