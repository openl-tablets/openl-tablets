package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.studio.projects.service.tables.TableTestProjects;

/** What a project leaves behind when a module of it is written to and the project is compiled again. */
class ProjectRecompilationTest {

    private static final String[][] BOTH_TABLES = {{"Method String hello(String name)"},
            {"Return \"Hi, \" + name;"},
            {null},
            {"Method String bye(String name)"},
            {"Return \"Bye, \" + name;"}};

    private static final String[][] ONE_TABLE = {{"Method String hello(String name)"},
            {"Return \"Hi, \" + name;"}};

    @Test
    void keepsTheSyntaxNodesOfOneCompilation(@TempDir Path dir) throws Exception {
        TableTestProjects.writeProject(dir, "Rules", "Rules", BOTH_TABLES);
        var module = ProjectResolver.getInstance().resolve(dir).getModules().getFirst();
        var model = openedModel(module);
        var afterFirst = syntaxNodesOf(model);

        // What a write to one of the module's tables forces: that module built from its workbook again, and
        // the project after it.
        TableTestProjects.writeProject(dir, "Rules", "Rules", ONE_TABLE);
        model.reset(ReloadType.SINGLE, module);
        awaitCompiled(model);

        // Compiling the whole project builds a syntax node of its own for it, which belongs to no dependency and
        // so is dropped by nothing. Gathered, they hold every compilation the session ever ran, workbooks and all.
        assertEquals(afterFirst, syntaxNodesOf(model), "a project keeps the nodes of one compilation");
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

    /**
     * How many syntax nodes the model holds for each project.
     *
     * <p>Read from the field itself. A node a compilation left behind carries the same tables as the node that
     * replaced it, so nothing the model answers with says how many of them it is holding on to.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> syntaxNodesOf(ProjectModel model) throws Exception {
        var field = ProjectModel.class.getDeclaredField("xlsModuleSyntaxNodesPerProject");
        field.setAccessible(true);
        var byProject = (Map<String, Set<XlsModuleSyntaxNode>>) field.get(model);
        return byProject.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }
}
