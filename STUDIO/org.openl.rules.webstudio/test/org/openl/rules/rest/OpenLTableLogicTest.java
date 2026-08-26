package org.openl.rules.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.rest.compile.OpenLTableLogic;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.tableeditor.TableBean;

class OpenLTableLogicTest {

    @Test
    void getTargetTables() throws Exception {
        WebStudio webStudio = mock(WebStudio.class);
        var pm = new ProjectModel(webStudio, null);
        pm.setModuleInfo(getModules().getFirst());
        Collection<TableSyntaxNode> allTableSyntaxNodes = pm.getAllTableSyntaxNodes();
        for (TableSyntaxNode tsn : allTableSyntaxNodes) {
            var tableSyntaxNodeAdapter = new TableSyntaxNodeAdapter(tsn);
            if (((IOpenLTable) tableSyntaxNodeAdapter).getDisplayName().equals("HelloTest")) {
                List<TableBean.TableDescription> targetTables = OpenLTableLogic.getTargetTables(tableSyntaxNodeAdapter,
                        pm, false);
                assertEquals(3, targetTables.size());
                assertEquals("Hello [state = AL]", targetTables.getFirst().getName());
                assertEquals("Hello [state = AZ]", targetTables.get(1).getName());
                assertEquals("Hello", targetTables.get(2).getName());
            }
        }
    }

    @Test
    void detectsErrorsInRulesTestedByTable() throws Exception {
        var webStudio = mock(WebStudio.class);
        var projectModel = spy(new ProjectModel(webStudio, null));
        projectModel.setModuleInfo(getModules().getFirst());
        var testTable = projectModel.getAllTableSyntaxNodes().stream()
                .map(TableSyntaxNodeAdapter::new)
                .filter(table -> table.getDisplayName().equals("HelloTest"))
                .findFirst()
                .orElseThrow();

        assertFalse(OpenLTableLogic.testedRulesHaveErrors(testTable, projectModel, false));

        doReturn(List.of(mock(OpenLMessage.class))).when(projectModel).getErrorsByUri(anyString());
        assertTrue(OpenLTableLogic.testedRulesHaveErrors(testTable, projectModel, false));
    }

    private List<Module> getModules() throws ProjectResolvingException {
        return ProjectResolver.getInstance().resolve(Path.of("test-resources/org/openl/rules/table")).getModules();
    }

}
