package org.openl.rules.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.rest.compile.OpenLTableLogic;
import org.openl.rules.rest.compile.TableDescription;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;

class OpenLTableLogicTest {

    @Test
    void getTargetTables() throws Exception {
        WebStudio webStudio = mock(WebStudio.class);
        var pm = new ProjectModel(webStudio, null);
        pm.setModuleInfo(getModules().getFirst());
        Collection<TableSyntaxNode> allTableSyntaxNodes = pm.getAllTableSyntaxNodes();
        for (TableSyntaxNode tsn : allTableSyntaxNodes) {
            var tableSyntaxNodeAdapter = new TableSyntaxNodeAdapter(tsn);
            if ("HelloTest".equals(tableSyntaxNodeAdapter.getDisplayName())) {
                List<TableDescription> targetTables = OpenLTableLogic.getTargetTables(tableSyntaxNodeAdapter,
                        pm, false);
                assertEquals(3, targetTables.size());
                // Every version is called by the name of the table, and told apart by its dimensions.
                assertEquals(List.of("Hello", "Hello", "Hello"),
                        targetTables.stream().map(TableDescription::name).toList());
                assertEquals(List.of(List.of("AL"), List.of("AZ"), List.of()), targetTables.stream()
                        .map(target -> states(target.dimensions()))
                        .toList());
            }
        }
    }

    /** The codes of the states a version is written for, none when it names no state. */
    private static List<String> states(Map<String, Object> dimensions) {
        var states = (Object[]) dimensions.get("state");
        return states == null ? List.of() : Arrays.stream(states).map(state -> ((Enum<?>) state).name()).toList();
    }

    @Test
    void detectsErrorsInRulesTestedByTable() throws Exception {
        var webStudio = mock(WebStudio.class);
        var projectModel = spy(new ProjectModel(webStudio, null));
        projectModel.setModuleInfo(getModules().getFirst());
        var testTable = projectModel.getAllTableSyntaxNodes().stream()
                .map(TableSyntaxNodeAdapter::new)
                .filter(table -> "HelloTest".equals(table.getDisplayName()))
                .findFirst()
                .orElseThrow();

        assertFalse(OpenLTableLogic.getTargetTables(testTable, projectModel, false).isEmpty(),
                "the fixture test table must name the rules it exercises");
        assertFalse(OpenLTableLogic.testedRulesHaveErrors(testTable, projectModel, false));

        doReturn(List.of(mock(OpenLMessage.class))).when(projectModel).getErrorsByUri(anyString());
        assertTrue(OpenLTableLogic.testedRulesHaveErrors(testTable, projectModel, false));
    }

    /**
     * The fixture modules, each set to compile on its own.
     *
     * <p>Opening a module otherwise starts a compilation of the whole project in the background, which replaces
     * what the model holds part-way through a test: a later question about a table is then answered from a model
     * that is compiling again, and the answer depends on which of the two got there first.
     */
    private List<Module> getModules() throws ProjectResolvingException {
        var modules = ProjectResolver.getInstance()
                .resolve(Path.of("test-resources/org/openl/rules/table"))
                .getModules();
        modules.forEach(module -> module.getWebstudioConfiguration().setCompileThisModuleOnly(true));
        return modules;
    }

}
