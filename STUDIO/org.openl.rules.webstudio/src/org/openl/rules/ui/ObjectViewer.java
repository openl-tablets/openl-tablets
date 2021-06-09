package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * @author snshor
 *
 * @deprecated
 */
@Deprecated
public final class ObjectViewer {

    private ObjectViewer() {
    }

    /** Display SpreadsheetResult with added filter for given fields as expected result and passed/failed icon **/
    public static String displaySpreadsheetResult(final SpreadsheetResult res,
            Map<Point, ComparedResult> spreadsheetCellsForTest,
            String requestId) {
        return display(res, spreadsheetCellsForTest, true, requestId, false);
    }

    /** Display SpreadsheetResult with filter for links to explanation for values */
    public static String displaySpreadsheetResult(final SpreadsheetResult res, String requestId) {
        return display(res, null, true, requestId, false);
    }

    /** Display SpreadsheetResult without any filters in the table **/
    public static String displaySpreadsheetResultNoFilters(final SpreadsheetResult res, boolean smartNumbers) {
        return display(res, null, false, null, smartNumbers);
    }

    private static String display(final SpreadsheetResult res,
            Map<Point, ComparedResult> spreadsheetCellsForTest,
            boolean filter,
            String requestId,
            boolean smartNumbers) {
        List<IGridFilter> filters = new ArrayList<>();
        filters.add(new TableValueFilter(res));
        filters.add(CollectionCellFilter.INSTANCE);

        if (filter) {
            filters.add(new LinkMaker(requestId));

            // Check if the cells for test are initialized,
            // Means Spreadsheet should be displayed with expected values for tests
            //
            if (spreadsheetCellsForTest != null) {
                filters.add(new ExpectedResultFilter(spreadsheetCellsForTest));
            }
        }

        ILogicalTable table = res.getLogicalTable();
        IGridTable gridtable = table.getSource();

        ProjectModel model = WebStudioUtils.getWebStudio().getModel();
        MetaInfoReader metaInfoReader = Optional.ofNullable(model.getNode(gridtable.getUri()))
            .map(TableSyntaxNode::getMetaInfoReader)
            .orElseGet(EmptyMetaInfoReader::getInstance);

        TableModel tableModel = TableModel
            .initializeTableModel(gridtable, filters.toArray(new IGridFilter[0]), metaInfoReader, smartNumbers);
        return new HTMLRenderer.TableRenderer(tableModel).render(false);
    }

}
