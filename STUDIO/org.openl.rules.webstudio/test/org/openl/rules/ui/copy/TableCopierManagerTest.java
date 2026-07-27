package org.openl.rules.ui.copy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

class TableCopierManagerTest {

    @Test
    void selectsACopierPerCopyType() {
        var table = mock(IOpenLTable.class);
        var properties = mock(ITableProperties.class);
        when(table.getName()).thenReturn("SourceTable");
        when(table.getType()).thenReturn(XlsNodeTypes.XLS_DT.toString());
        when(table.getProperties()).thenReturn(properties);
        when(properties.getTableProperties()).thenReturn(java.util.Collections.emptyMap());

        var workbookSource = mock(XlsWorkbookSourceCodeModule.class);
        when(workbookSource.getDisplayName()).thenReturn("Main.xlsx");
        var workbookNode = mock(WorkbookSyntaxNode.class);
        when(workbookNode.getWorkbookSourceCodeModule()).thenReturn(workbookSource);
        var moduleNode = mock(XlsModuleSyntaxNode.class);
        when(moduleNode.getWorkbookSyntaxNodes()).thenReturn(new WorkbookSyntaxNode[]{workbookNode});

        var projectModel = mock(ProjectModel.class);
        when(projectModel.getAllEditableWorkbookNodes()).thenReturn(List.of(workbookNode));
        when(projectModel.getXlsModuleNode()).thenReturn(moduleNode);
        when(projectModel.getTable("table://source")).thenReturn(table);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getModel()).thenReturn(projectModel);
        when(webStudio.getTableUri()).thenReturn("table://source");

        try (var webStudioUtils = mockStatic(WebStudioUtils.class)) {
            webStudioUtils.when(WebStudioUtils::getWebStudio).thenReturn(webStudio);
            webStudioUtils.when(WebStudioUtils::getOrCreateWebStudio).thenReturn(webStudio);
            webStudioUtils.when(WebStudioUtils::getProjectModel).thenReturn(projectModel);
            webStudioUtils.when(() -> WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID)).thenReturn(null);

            var manager = new TableCopierManager();

            assertNull(manager.start());
            assertEquals("CHANGE_NAMES", manager.getCopyType());
            assertInstanceOf(TableNamesCopier.class, manager.getCopier());

            manager.setCopyType("CHANGE_DIMENSION");
            assertInstanceOf(DimensionalPropertiesTableCopier.class, manager.getCopier());

            manager.setCopyType("CHANGE_VERSION");
            assertInstanceOf(VersionPropertyTableCopier.class, manager.getCopier());

            // Switching the copy type starts over from the table being copied, not from what a previous copier held.
            assertEquals(table, manager.getCopier().getCopyingTable());
        }
    }
}
