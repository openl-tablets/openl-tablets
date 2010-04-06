/**
 * Created Jan 5, 2007
 */
package org.openl.rules.ui;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.openl.base.INamedThing;
import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaHolder;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.validator.DecisionTableOverlapping;
import org.openl.rules.dt.validator.DecisionTableUncovered;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.search.ISearchTableRow;
import org.openl.rules.search.OpenLAdvancedSearchResult;
import org.openl.rules.search.OpenLAdvancedSearchResultViewer;
import org.openl.rules.search.OpenLBussinessSearchResult;
import org.openl.rules.search.OpenLAdvancedSearchResult.TableAndRows;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Table;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.ui.filters.SimpleHtmlFilter;
import org.openl.rules.table.ui.filters.TableValueFilter;
import org.openl.rules.table.ui.filters.XlsSimpleFilter;
import org.openl.rules.table.xls.formatters.AXlsFormatter;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.ui.search.TableSearch;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webtools.WebTool;
import org.openl.util.StringTool;

/**
 * @author snshor
 */
public class ObjectViewer {

    private ProjectModel projectModel;

    public ObjectViewer(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    public static Object displaySpreadsheetResult(final SpreadsheetResult res) {

        TableSyntaxNode tsn = (TableSyntaxNode) res.getSpreadsheet().getInfo().getSyntaxNode();

        ILogicalTable table = tsn.getTableBody();
        IGridTable gt = table.getGridTable();

        final int firstRowHeight = table.getLogicalRow(0).getGridTable().getGridHeight();
        final int firstColWidth = table.getLogicalColumn(0).getGridTable().getGridWidth();

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
        IGridFilter[] filters = { tvf, new XlsSimpleFilter(), new SimpleHtmlFilter(), new LinkMaker(tvf) };

        FilteredGrid fg = new FilteredGrid(gt.getGrid(), filters);

        // AB: show only results
        // return new Object[]{gt, new GridTable(gt.getRegion(), fg)};
        return new GridTable(gt.getRegion(), fg);
    }

    public static StringBuffer makeExcelLink(IGridTable table, String text, StringBuffer buf) {
        String uri = table.getUri();
        String url = WebTool.makeXlsOrDocUrl(table.getUri());

        buf.append("<img src='webresource/images/excel-workbook.png'/>");
        buf.append("<a class='left' href='showLinks.jsp?" + url + "' target='show_app_hidden' title='" + uri + "'>"
                + "&nbsp;" + text + "</a>");

        return buf;
    }

    private String displayArray(Object res) {
        int len = Array.getLength(res);
        if (len == 0) {
            return "[]";
        }

        Object el = Array.get(res, 0);
        if (el == null) {
            return "[]";
        }

        if (el.getClass().isArray()) {
            return displayMatrix(res);
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < len; i++) {
            buf.append("<p>\n");
            buf.append(displayResult(Array.get(res, i)));
        }

        return buf.toString();
    }

    private String displayDTValidationResult(DesionTableValidationResult res) {

        DecisionTable dt = res.getDecisionTable();
        TableSyntaxNode tsn = (TableSyntaxNode) dt.getSyntaxNode();
        IGridTable gt = tsn.getTable().getGridTable();

        StringBuffer buf = new StringBuffer(1000);

        if (res.getOverlappings().length == 0 && res.getUncovered().length == 0) {
            return makeExcelLink(gt, "<b>Table is Complete and has no Overlappings</b>", buf).toString();
        }

        buf.append("<p/>");

        if (res.getOverlappings().length > 0) {
            DecisionTableOverlapping ov = res.getOverlappings()[0];
            buf.append("<p>").append("The following sample input data will trigger overlapping rules ");

            int[] rules = ov.getRules();
            for (int i = 0; i < rules.length; i++) {
                buf.append("<b>").append(i > 0 ? ", " : "").append(dt.getRuleName(rules[i])).append("</b>");
            }

            buf.append("</p>");

            buf.append("<table style='border: 1px solid'><tr>");
            int size = ov.getValue().size();

            for (int i = 0; i < size; i++) {
                buf.append("<th class='data_header'>").append(ov.getValue().getName(i)).append("</th>");
            }
            buf.append("</tr><tr>");

            for (int i = 0; i < size; i++) {
                buf.append("<td class='data_cell'>").append(ov.getValue().getValue(i)).append("</td>");
            }

            buf.append("</tr></table><p/>");

            IGridFilter cf = makeFilter(ov.getRules(), res.getDecisionTable());

            String type = IXlsTableNames.VIEW_BUSINESS;
            ILogicalTable gtx = tsn.getSubTables().get(type);
            if (gtx != null) {
                gt = gtx.getGridTable();
            }

            buf.append(ProjectModel.showTable(gt, new IGridFilter[] { cf }, false));
        }

        if (res.getUncovered().length > 0) {
            buf.append("<p>").append("The following sample input data will not trigger any rules(Undercoverage): ");

            buf.append("</p>");

            DecisionTableUncovered un = res.getUncovered()[0];
            buf.append("<table style='border: 1px solid'><tr>");
            int size = un.getValues().size();

            for (int i = 0; i < size; i++) {
                buf.append("<th class='data_header'>").append(un.getValues().getName(i)).append("</th>");
            }
            buf.append("</tr><tr>");

            for (int i = 0; i < size; i++) {
                buf.append("<td class='data_cell'>").append(un.getValues().getValue(i)).append("</td>");
            }

            buf.append("</tr></table><p/>");

            String type = "view.business";
            ILogicalTable gtx = tsn.getSubTables().get(type);
            if (gtx != null) {
                gt = gtx.getGridTable();
            }

            buf.append(ProjectModel.showTable(gt, false));

        }
        return buf.toString();
    }

    private String displayMatrix(Object res) {
        int len = Array.getLength(res);

        StringBuffer buf = new StringBuffer();

        buf.append("<table class='ov-matrix'>\n");

        for (int i = 0; i < len; i++) {

            Object row = Array.get(res, i);
            if (row == null) {
                continue;
            }

            buf.append("<tr>\n");
            int l2 = Array.getLength(row);

            for (int j = 0; j < l2; j++) {
                Object x = Array.get(row, j);
                buf.append("<td class='ov-matrix'>");
                buf.append(displayResult(x));
                buf.append("</td>");
            }

            buf.append("</tr>\n");

        }

        buf.append("</table>\n");

        return buf.toString();
    }

    public StringBuffer displayMetaHolder(IMetaHolder mh, String display, StringBuffer buf) {
        buf.append("<a ");
        makeXlsOrDocUrl(mh.getMetaInfo().getSourceUrl(), buf);
        buf.append(">");
        StringTool.encodeHTMLBody(display, buf);
        buf.append("</a>");

        return buf;

    }

    public String displayResult(Object res) {
        if (res == null) {
            return "<b>null</b>";
        }

        if (res.getClass().isArray()) {
            return displayArray(res);
        }

        if (res instanceof IMetaHolder) {
            return displayMetaHolder((IMetaHolder) res, String.valueOf(res), new StringBuffer()).toString();
        }

        if (res instanceof DesionTableValidationResult) {
            return displayDTValidationResult((DesionTableValidationResult) res);
        }

        if (res instanceof IGridTable) {
            IGridTable tt = (IGridTable) res;

            return ProjectModel.showTable(tt, false);
        }

        if (res instanceof GridWithNode) {
            if (projectModel != null) {
                TableSyntaxNode tsn = ((GridWithNode) res).tableSyntaxNode;
                String uri = tsn != null ? tsn.getUri() : null;
                String nodeKey = projectModel.getTreeNodeId(uri);
                if (StringUtils.isNotBlank(nodeKey)) {
                    TableModel tableModel = ProjectModel.buildModel(((GridWithNode) res).gridTable, null);
                    HTMLRenderer.TableRenderer renderer = new HTMLRenderer.TableRenderer(tableModel);
                    renderer.setCellIdPrefix("cell-" + nodeKey + "-");
                    //FIXME: should formulas be displayed?
                    return renderer.renderWithMenu(null, false, null);
                }
            }

            return ProjectModel.showTable(((GridWithNode) res).gridTable, false);
        }

        if (res instanceof OpenLAdvancedSearchResult) {
            OpenLAdvancedSearchResult srch = (OpenLAdvancedSearchResult) res;
            return displaySearch(srch);
        }

        if (res instanceof INamedThing) {
            return ((INamedThing) res).getName();
        }

        if (res instanceof SpreadsheetResult) {
            SpreadsheetResult sres = (SpreadsheetResult) res;
            return displayResult(displaySpreadsheetResult(sres));
        }

        String value = res.toString();

        return value;
    }

    public String displaySearch(OpenLAdvancedSearchResult searchRes) {
        TableAndRows[] tr = searchRes.tablesAndRows();

        Object[] res = new Object[tr.length * 2];
        OpenLAdvancedSearchResultViewer sviewer = new OpenLAdvancedSearchResultViewer(searchRes);

        for (int i = 0; i < tr.length; i++) {
            TableSyntaxNode tsn = tr[i].getTsn();

            StringValue tname = TableSyntaxNodeUtils.getTableSyntaxNodeName(tsn);
            res[2 * i] = tname;

            ISearchTableRow[] trows = tr[i].getRows();
            CompositeGrid cg = sviewer.makeGrid(trows);
            res[2 * i + 1] = cg != null ? new GridWithNode(cg.asGridTable(), tsn) : "No rows selected";
        }

        return displayResult(res);
    }

    public List<TableSearch> getSearchList(Object searchRes) {
        List<TableSearch> tableSearchList = new ArrayList<TableSearch>();
        if (searchRes instanceof OpenLAdvancedSearchResult) {
            TableAndRows[] tr = ((OpenLAdvancedSearchResult) searchRes).tablesAndRows();
            OpenLAdvancedSearchResultViewer sviewer = new OpenLAdvancedSearchResultViewer(
                    (OpenLAdvancedSearchResult) searchRes);
            for (int i = 0; i < tr.length; i++) {
                ISearchTableRow[] rows = tr[i].getRows();
                if (rows.length > 0) {
                    TableSyntaxNode tsn = tr[i].getTsn();
                    StringValue tableName = TableSyntaxNodeUtils.getTableSyntaxNodeName(tsn);
                    String tableUri = tsn.getUri();
                    CompositeGrid cg = sviewer.makeGrid(rows);
                    IGridTable gridTable = cg != null ? cg.asGridTable() : null;
                    Table newTable = new Table();
                    newTable.setGridTable(gridTable);
                    newTable.setProperties(tsn.getTableProperties());
                    TableSearch tableSearch = new TableSearch();
                    tableSearch.setTableUri(tableUri);
                    tableSearch.setTable(newTable);
                    tableSearch.setXlsLink((displayResult(tableName)));
                    tableSearchList.add(tableSearch);
                }
            }
        }
        if (searchRes instanceof OpenLBussinessSearchResult) {
            List<TableSyntaxNode> foundTables = ((OpenLBussinessSearchResult) searchRes).getFoundTables();
            for(TableSyntaxNode foundTable : foundTables) {
                TableSearch tableSearch = new TableSearch();
                tableSearch.setTableUri(foundTable.getUri());
                tableSearch.setTable(new TableSyntaxNodeAdapter(foundTable));
                tableSearch.setXlsLink((displayResult(TableSyntaxNodeUtils.getTableSyntaxNodeName(foundTable))));
                tableSearchList.add(tableSearch);                
            }
        }
        return tableSearchList;
    }
    
    private IGridFilter makeFilter(int[] rules, DecisionTable dt) {
        IGridRegion[] regions = new IGridRegion[rules.length];

        for (int i = 0; i < rules.length; i++) {
            regions[i] = dt.getRuleTable(rules[i]).getGridTable().getRegion();
        }

        ColorFilterHolder cf = projectModel == null ? new ColorFilterHolder() : projectModel.getFilterHolder();
        return new ColorGridFilter(new RegionGridSelector(regions, true), cf.makeFilter());
    }

    private void makeXlsOrDocUrl(String uri, StringBuffer buf) {
        String url = WebTool.makeXlsOrDocUrl(uri);
        buf.append("href='" + WebContext.getContextPath() + "/jsp/showLinks.jsp?").append(url).append("'");
        buf.append(" target='show_app_hidden'");
    }

    private static class GridWithNode {
        IGridTable gridTable;
        TableSyntaxNode tableSyntaxNode;

        private GridWithNode(IGridTable gridTable, TableSyntaxNode tableSyntaxNode) {
            this.gridTable = gridTable;
            this.tableSyntaxNode = tableSyntaxNode;
        }
    }

    private static class LinkMaker implements IGridFilter, IGridSelector {

        private String url;

        private TableValueFilter dataAdapter;

        public LinkMaker(TableValueFilter dataAdapter) {
            super();
            this.dataAdapter = dataAdapter;
        }

        public FormattedCell filterFormat(FormattedCell cell) {

            String fontStyle = WebTool.fontToHtml(cell.getFont(), new StringBuilder()).toString();

            cell.setFormattedValue("<a href=\"" + url + "\" class=\"nounderline\" style=\"" + fontStyle + "\"  >"
                    + cell.getFormattedValue() + "</a>");
            return cell;
        }

        public IGridSelector getGridSelector() {
            return this;
        }

        private String makeUrl(int col, int row, TableValueFilter dataAdapter) {
            Object obj = dataAdapter.getCellValue(col, row);

            if (obj == null || !(obj instanceof DoubleValue)) {
                return null;
            }

            DoubleValue dv = (DoubleValue) obj;
            if (Math.abs(dv.doubleValue()) < 0.005) {
                return null;
            }

            return getURL(dv);
        }

        public static String getURL(DoubleValue dv) {
            int rootID = Explanator.getCurrent().getUniqueId(dv);
            return "javascript: open_explain_win(\'?rootID=" + rootID + "&header=Explanation')";
        }

        public Object parse(String value) {
            return value;
        }

        public boolean selectCoords(int col, int row) {
            url = makeUrl(col, row, dataAdapter);

            return url != null;
        }

        public AXlsFormatter getFormatter() {
            return null;
        }

    }

}
