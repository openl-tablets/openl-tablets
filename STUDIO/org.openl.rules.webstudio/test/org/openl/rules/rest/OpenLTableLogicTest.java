package org.openl.rules.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
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

public class OpenLTableLogicTest {

    @Test
    public void getTargetTables() throws Exception {
        WebStudio webStudio = mock(WebStudio.class);
        ProjectModel pm = new ProjectModel(webStudio, null);
        pm.setModuleInfo(getModules().get(0));
        Collection<TableSyntaxNode> allTableSyntaxNodes = pm.getAllTableSyntaxNodes();
        for (TableSyntaxNode tsn : allTableSyntaxNodes) {
            TableSyntaxNodeAdapter tableSyntaxNodeAdapter = new TableSyntaxNodeAdapter(tsn);
            if (((IOpenLTable) tableSyntaxNodeAdapter).getDisplayName().equals("HelloTest")) {
                List<TableBean.TableDescription> targetTables = OpenLTableLogic.getTargetTables(tableSyntaxNodeAdapter,
                    pm, false);
                assertEquals(3, targetTables.size());
                assertEquals("Hello [state = AL]", targetTables.get(0).getName());
                assertEquals("Hello [state = AZ]", targetTables.get(1).getName());
                assertEquals("Hello", targetTables.get(2).getName());
            }
        }
    }

    private List<Module> getModules() throws ProjectResolvingException {
        return ProjectResolver.getInstance().resolve(new File("test-resources/org/openl/rules/table")).getModules();
    }

}
