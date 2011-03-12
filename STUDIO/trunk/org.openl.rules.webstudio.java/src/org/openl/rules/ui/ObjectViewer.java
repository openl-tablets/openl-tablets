/**
 * Created Jan 5, 2007
 */
package org.openl.rules.ui;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.ui.filters.TableValueFilter;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;

/**
 * @author snshor
 * 
 * @deprecated
 */
public class ObjectViewer {

    public ObjectViewer() {
    }

    public static String displaySpreadsheetResult(final SpreadsheetResult res) {
        ILogicalTable table = res.getLogicalTable();
        IGridTable gt = table.getSource();

        final int firstRowHeight = table.getRow(0).getSource().getHeight();
        final int firstColWidth = table.getColumn(0).getSource().getWidth();

        TableValueFilter.Model model = new TableValueFilter.Model() {

            public Object getValue(int col, int row) {
                if (row < firstRowHeight) {
                    return null; // the row 0 contains column headers
                }
                if (col < firstColWidth) {
                    return null;
                }
                if (res.width() <= col - firstColWidth || res.height() <= row - firstRowHeight) {
                    return null;
                }

                return res.getValue(row - firstRowHeight, col - firstColWidth);
            }

        };

        TableValueFilter tvf = new TableValueFilter(gt, model);
        IGridFilter[] filters = { tvf, new LinkMaker(tvf) };

        TableModel tableModel = TableModel.initializeTableModel(gt, filters);
        return new HTMLRenderer.TableRenderer(tableModel).render(false);
    }

    private static class LinkMaker implements IGridFilter, IGridSelector {

        private String url;

        private TableValueFilter dataAdapter;

        public LinkMaker(TableValueFilter dataAdapter) {
            super();
            this.dataAdapter = dataAdapter;
        }

        public FormattedCell filterFormat(FormattedCell cell) {

            String fontStyle = CellModel.fontToHtml(cell.getFont(), new StringBuilder()).toString();

            cell.setFormattedValue("<a href=\"" + url + "\" class=\"nounderline\" style=\"" + fontStyle + "\"  >"
                    + cell.getFormattedValue() + "</a>");
            return cell;
        }

        public IGridSelector getGridSelector() {
            return this;
        }

        private String makeUrl(int col, int row, TableValueFilter dataAdapter) {
            Object obj = dataAdapter.getCellValue(col, row);
            
            if (obj == null || !(obj instanceof ExplanationNumberValue<?>)) {
                return null;
            }

            ExplanationNumberValue<?> explanationValue = (ExplanationNumberValue<?>) obj;
            if (Math.abs(explanationValue.doubleValue()) < 0.005) {
                return null;
            }

            return getURL(explanationValue);
        }

        public static String getURL(ExplanationNumberValue<?> dv) {
            int rootID = Explanator.getCurrent().getUniqueId(dv);
            return "javascript: open_explain_win(\'?rootID=" + rootID + "&header=Explanation')";
        }

        public boolean selectCoords(int col, int row) {
            url = makeUrl(col, row, dataAdapter);

            return url != null;
        }

    }

}
