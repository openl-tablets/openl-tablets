package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import jakarta.faces.context.ExternalContext;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

class SearchBeanTest {

    @Test
    void searchUsesLocalCellValues() {
        var matching = table(XlsNodeTypes.XLS_DT.toString(), "matching value");
        var nonMatching = table(XlsNodeTypes.XLS_DT.toString(), "other value");
        var tablePart = table(XlsNodeTypes.XLS_TABLEPART.toString(), "matching value");
        var gapOverlap = table(XlsNodeTypes.XLS_DT.toString(), "matching value");

        var externalContext = mock(ExternalContext.class);
        when(externalContext.getRequestPathInfo()).thenReturn("/search.xhtml");
        var webStudio = mock(WebStudio.class);
        var projectModel = mock(ProjectModel.class);
        when(projectModel.getSearchScopeData(SearchScope.CURRENT_PROJECT))
                .thenReturn(new LinkedHashSet<>(List.of(matching, nonMatching, tablePart, gapOverlap)));
        when(projectModel.isGapOverlap(gapOverlap)).thenReturn(true);

        try (var webStudioUtils = mockStatic(WebStudioUtils.class)) {
            webStudioUtils.when(WebStudioUtils::getExternalContext).thenReturn(externalContext);
            webStudioUtils.when(WebStudioUtils::getWebStudio).thenReturn(webStudio);
            webStudioUtils.when(WebStudioUtils::getProjectModel).thenReturn(projectModel);
            webStudioUtils.when(() -> WebStudioUtils.getRequestParameter("query")).thenReturn("matching");

            var searchBean = new SearchBean();

            assertEquals(1, searchBean.getSearchResults().size());
            assertSame(matching, searchBean.getSearchResults().getFirst().getSyntaxNode());
            verify(projectModel).compileProject(true, true);
        }
    }

    private static TableSyntaxNode table(String type, Object value) {
        var cell = mock(ICell.class);
        when(cell.getObjectValue()).thenReturn(value);
        var grid = mock(IGridTable.class);
        when(grid.getWidth()).thenReturn(1);
        when(grid.getHeight()).thenReturn(1);
        when(grid.getCell(0, 0)).thenReturn(cell);
        var table = mock(TableSyntaxNode.class);
        when(table.getType()).thenReturn(type);
        when(table.getGridTable()).thenReturn(grid);
        return table;
    }
}
