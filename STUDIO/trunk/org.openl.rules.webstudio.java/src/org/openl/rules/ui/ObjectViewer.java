/**
 * Created Jan 5, 2007
 */
package org.openl.rules.ui;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.IOpenSourceCodeModule;
import org.openl.base.INamedThing;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.BoundError;
import org.openl.main.SourceCodeURLTool;
import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaHolder;
import org.openl.meta.OpenLRuntimeExceptionWithMetaInfo;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.rules.dt.DTOverlapping;
import org.openl.rules.dt.DTRule;
import org.openl.rules.dt.DTUncovered;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
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
import org.openl.rules.table.ui.ColorGridFilter;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.SimpleHtmlFilter;
import org.openl.rules.table.ui.TableValueFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.testmethod.TestResult;
import org.openl.rules.ui.search.TableSearch;
import org.openl.rules.validator.dt.DTValidationResult;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webstudio.web.tableeditor.ShowTableBean;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.webtools.XlsUrlParser;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.SyntaxErrorException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DynamicObject;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringTool;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.openl.rules.webstudio.web.tableeditor.*;

/**
 * @author snshor
 */
public class ObjectViewer {
    private static final Log LOG = LogFactory.getLog(ObjectViewer.class);
    
    private static class GridWithNode {
        IGridTable gridTable;
        TableSyntaxNode tableSyntaxNode;

        private GridWithNode(IGridTable gridTable, TableSyntaxNode tableSyntaxNode) {
            this.gridTable = gridTable;
            this.tableSyntaxNode = tableSyntaxNode;
        }
    }

    static class LinkMaker implements IGridFilter, IGridSelector {

        String url;

        TableValueFilter dataAdapter;

        public LinkMaker(TableValueFilter dataAdapter) {
            super();
            this.dataAdapter = dataAdapter;
        }

        public FormattedCell filterFormat(FormattedCell cell) {

            String fontStyle = WebTool.fontToHtml(cell.getFont(), new StringBuffer()).toString();

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

        public Object parse(String value) {
            return value;
        }

        public boolean selectCoords(int col, int row) {
            url = makeUrl(col, row, dataAdapter);

            return url != null;
        }

    }

    // public ObjectViewer() {}

    static NumberFormat format = new DecimalFormat("#.0#");

    private ProjectModel projectModel;

    static public Object displaySpreadsheetResult(final SpreadsheetResult res) {

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
        IGridFilter[] filters = { tvf, new SimpleXlsFormatter(), new SimpleHtmlFilter(), new LinkMaker(tvf) };

        FilteredGrid fg = new FilteredGrid(gt.getGrid(), filters);

        // AB: show only results
        // return new Object[]{gt, new GridTable(gt.getRegion(), fg)};
        return new GridTable(gt.getRegion(), fg);
    }

    public static String getURL(DoubleValue dv) {
        int rootID = Explanator.getCurrent().getUniqueId(dv);
        return "javascript: open_explain_win(\'?rootID=" + rootID + "&header=Explanation')";
    }

    public static StringBuffer makeExcelLink(IGridTable table, String text, StringBuffer buf) {
        String uri = table.getUri();
        String url = WebTool.makeXlsOrDocUrl(table.getUri());

        buf.append("<img src='webresource/images/excel-workbook.png'/>");
        buf.append("<a class='left' href='showLinks.jsp?" + url + "' target='show_app_hidden' title='" + uri + "'>"
                + "&nbsp;" + text + "</a>");

        return buf;

    }

    public static StringBuffer printCodeAndErrorToHtml(ILocation location,
            IOpenSourceCodeModule module, StringBuffer buf) {
        String src = null;
        int pstart = 0;
        int pend = 0;
        
        if (location == null || !location.isTextLocation()) {
            try {
                src = module.getCode();
            } catch (NullPointerException e) {
                LOG.info("Can't get code with error", e);
                // suppress if we can't show code with error
            } catch (UnsupportedOperationException e) {
                LOG.info("Showing code with error is not supported", e);
                // suppress if we can't show code with error
            }
            if (src == null || src.trim().length() == 0) {
                src = StringUtils.EMPTY;
            }
            pstart = 0;
            pend = src.length();
        } else {
            src = module.getCode();
            TextInfo info = new TextInfo(src);
            pstart = location.getStart().getAbsolutePosition(info);
            pend = Math.min(location.getEnd().getAbsolutePosition(info) + 1,
                    src.length());
        }

        buf.append("\n<pre>\n");
        buf.append(StringEscapeUtils.escapeHtml(src.substring(0, pstart)));

        buf.append("<span class='codeerror'>");
        buf.append(StringEscapeUtils.escapeHtml(src.substring(pstart, pend)));
        buf.append("</span>");
        if (pend < src.length()) {
            ;
        }
        buf.append(StringEscapeUtils.escapeHtml(src.substring(pend, src.length())));

        buf.append("</pre>\n");

        return buf;

    }

    public ObjectViewer(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    /**
     * @param res
     * @return
     */
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

    public String displayDoubleValueWithExplanation(DoubleValue dv) {
        return displayDoubleValueWithExplanation(dv, format, null);
    }

    public String displayDoubleValueWithExplanation(DoubleValue dv, NumberFormat format, String clazz) {
        return "<a href=\"" + getURL(dv) + "\"" + (clazz == null ? "" : " class=\"" + clazz + "\"") + ">"
        // + new
                // String2DataConvertorFactory.String2DoubleConvertor().format(dv,dv.getFormat())
                + format.format(dv) + "</a>";
    }

    public String displayDoubleValueWithExplanation(DoubleValue dv, String clazz) {
        return displayDoubleValueWithExplanation(dv, format, clazz);
    }

    /**
     * @param res
     * @return
     */
    private String displayDTValidationResult(DTValidationResult res) {

        DecisionTable dt = res.getDT();
        TableSyntaxNode tsn = (TableSyntaxNode) dt.getSyntaxNode();
        IGridTable gt = tsn.getTable().getGridTable();

        StringBuffer buf = new StringBuffer(1000);

        if (res.getOverlappings().length == 0 && res.getUncovered().length == 0) {
            return makeExcelLink(gt, "<b>Table is Complete and has no Overlappings</b>", buf).toString();
        }

        // makeExcelLink(gt, "Show Table In Excel", buf);

        buf.append("<p/>");

        if (res.getOverlappings().length > 0) {
            DTOverlapping ov = res.getOverlappings()[0];
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

            IGridFilter cf = makeFilter(ov.getRules(), res.getDT());

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

            DTUncovered un = res.getUncovered()[0];
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

    private void displayErrorAndCode(Throwable tt, ILocation srcLocation, IOpenSourceCodeModule module, StringBuffer buf) {
        buf.append("<table><tr><td class='error_box'>");
        displayNonOpenlException(tt, buf).append("<p/>\n");
        String errorUri = SourceCodeURLTool.makeSourceLocationURL(srcLocation, module, "");
        String errorUrl = errorUri == null || "NO_MODULE".equals(errorUri) ? "NO+URL" : WebTool
                .makeXlsOrDocUrl(errorUri);
        String tableUri = null;
        TableSyntaxNode tableSyntaxNode = projectModel.findNode(errorUri);
        if (tableSyntaxNode != null) {
            tableUri = tableSyntaxNode.getUri();
        }
        XlsUrlParser uriParser = new XlsUrlParser();
        uriParser.parse(errorUri);
        buf.append("\n" + "<table><tr><td class='javacode'>");

        printCodeAndErrorToHtml(srcLocation, module, buf);

        boolean canEditTable = ShowTableBean.canModifyCurrentProject();
        if (canEditTable) {
            buf.append("<a href='javascript:editError(\"").append(
                    tableUri != null ? tableUri : "").append("\"").append(",")
                    .append("\"").append(uriParser.range).append(
                            "\")' title='Edit cell containing error'>");
            buf.append("Edit Table");
            buf.append(" </a>");
            
            buf.append("<br />");

            buf.append(
                    "<a href='" + WebContext.getContextPath()
                            + "/jsp/showLinks.jsp?").append(errorUrl).append(
                    "' target='show_app_hidden' title='").append(errorUri)
                    .append("'>");
            buf.append("Edit in Excel");
            buf.append(" </a>");
        } else {
            buf.append("<a class='left' href='javascript:viewError(\"").append(
                    tableUri != null ? tableUri : "").append("\"").append(",")
                    .append("\"").append(uriParser.range).append(
                            "\")' title='View table containing error'>");
            buf.append("View Table");
            buf.append("</a>");
        }
        
        buf.append("</td></tr></table>");
        buf.append("</td></tr></table>");
    }

    /**
     * @param t
     * @param buf
     * @return
     */
    private StringBuffer displayException(Throwable t, StringBuffer buf) {

        if (t instanceof OpenLRuntimeException) {
            OpenLRuntimeException ort = (OpenLRuntimeException) t;

            Throwable[] tt = ExceptionUtils.getThrowables(ort);

            for (int i = 1; i < tt.length; i++) {
                if (tt[i] instanceof OpenLRuntimeException || tt[i] instanceof SyntaxErrorException
                        || tt[i] instanceof BoundError || tt[i] instanceof OpenLRuntimeExceptionWithMetaInfo) {
                    return displayException(tt[i], buf);
                }
            }

            ISyntaxNode syntaxNode = ort.getNode().getSyntaxNode();

            ILocation srcLocation = syntaxNode.getSourceLocation();

            Throwable cause = ExceptionUtils.getRootCause(ort);

            // String msg = tt.getMessage();

            displayErrorAndCode(cause, srcLocation, syntaxNode.getModule(), buf);
            return buf;

        }

        if (t instanceof ISyntaxError) {
            ISyntaxError se = (ISyntaxError) t;

            displayErrorAndCode(t, se.getLocation(), se.getModule(), buf);
            return buf;
        }

        if (t instanceof SyntaxErrorException) {
            SyntaxErrorException se = (SyntaxErrorException) t;
            ISyntaxError[] err = se.getSyntaxErrors();

            for (int i = 0; i < err.length; i++) {
                displayErrorAndCode((Throwable) err[i], err[i].getLocation(), err[i].getModule(), buf);
            }

            return buf;
        }

        if (t instanceof OpenLRuntimeExceptionWithMetaInfo) {
            displayNonOpenlException(t, buf);
            OpenLRuntimeExceptionWithMetaInfo omi = (OpenLRuntimeExceptionWithMetaInfo) t;
            IMetaHolder[] holders = omi.getHolders();
            String[] descrs = omi.optionalDescriptions();
            for (int i = 0; i < holders.length; i++) {
                buf.append("<p/>");
                String descr = descrs == null ? stringValueof(holders[i]) : descrs[i];
                displayMetaHolder(holders[i], descr, buf);
            }

            return buf;
        }

        return displayNonOpenlException(t, buf);
    }

    /**
     * @param res
     * @return
     */
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

    private StringBuffer displayNonOpenlException(Throwable t, StringBuffer buf) {
        buf.append("<span class=\"codeerror\"><b>");
        buf.append(t.getClass().getName()).append(": ").append("</b></span>");

        toHtml(t.getMessage(), buf);
        return buf;
    }

    public String displayResult(Object res) {
        if (res == null) {
            return "<b>null</b>";
        }

        if (res.getClass().equals(new DTRule[0].getClass())) {
            return displayRuleArray((DTRule[]) res);
        }

        if (res.getClass().isArray()) {
            return displayArray(res);
        }

        if (res instanceof DoubleValue) {
            return displayDoubleValueWithExplanation((DoubleValue) res, format, null);
        }

        if (res instanceof IMetaHolder) {
            return displayMetaHolder((IMetaHolder) res, stringValueof(res), new StringBuffer()).toString();
        }

        if (res instanceof TestResult) {
            TestResult tt = (TestResult) res;
            return displayTestResult(tt);
        }

        if (res instanceof DTValidationResult) {
            return displayDTValidationResult((DTValidationResult) res);
        }

        if (res instanceof DTRule) {
            return displayRule((DTRule) res);
        }

        if (res instanceof IGridTable) {
            IGridTable tt = (IGridTable) res;

            return ProjectModel.showTable(tt, false);
        }

        if (res instanceof GridWithNode) {
            if (projectModel != null) {
                int index = projectModel.indexForNode(((GridWithNode) res).tableSyntaxNode);
                if (index >= 0) {
                    TableModel tableModel = ProjectModel.buildModel(((GridWithNode) res).gridTable, null);
                    HTMLRenderer.TableRenderer renderer = new HTMLRenderer.TableRenderer(tableModel);
                    renderer.setCellIdPrefix("cell-" + index + "-");
                    //FIXME: should formulas be displayed?
                    return renderer.renderWithMenu(null, false);
                }
            }

            return ProjectModel.showTable(((GridWithNode) res).gridTable, false);
        }

        if (res instanceof OpenLAdvancedSearchResult) {
            OpenLAdvancedSearchResult srch = (OpenLAdvancedSearchResult) res;
            return displaySearch(srch);
        }

        if (res instanceof Throwable) {
            Throwable t = (Throwable) res;
            StringBuffer buf = new StringBuffer(1000);
            displayException(t, buf);
            return buf.toString();
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

    /**
     * @param rule
     * @return
     */
    public String displayRule(DTRule rule) {
        StringBuffer buf = new StringBuffer(300);

        buf.append("<a class='left' ");
        makeXlsOrDocUrl(rule.getUri(), buf);
        buf.append(">");

        rule.display(buf, "html");
        buf.append("</a>");
        return buf.toString();
    }

    /**
     * @param rules
     * @return
     */
    public String displayRuleArray(DTRule[] rules) {
        IGridTable[] tables = new IGridTable[rules.length];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = rules[i].getGridTable();
        }

        CompositeGrid cg = new CompositeGrid(tables, true);
        GridTable gt = new GridTable(0, 0, cg.getHeight() - 1, cg.getWidth() - 1, cg);
        return displayResult(gt);
    }

    /**
     * @param searchRes
     * @return
     */
    public String displaySearch(OpenLAdvancedSearchResult searchRes) {
        TableAndRows[] tr = searchRes.tablesAndRows();

        Object[] res = new Object[tr.length * 2];
        OpenLAdvancedSearchResultViewer sviewer = new OpenLAdvancedSearchResultViewer(searchRes);

        for (int i = 0; i < tr.length; i++) {
            TableSyntaxNode tsn = tr[i].getTsn();

            StringValue tname = getTableName(tsn);
            res[2 * i] = tname;

            ISearchTableRow[] trows = tr[i].getRows();
            CompositeGrid cg = sviewer.makeGrid(trows);
            res[2 * i + 1] = cg != null ? new GridWithNode(cg.asGridTable(), tsn) : "No rows selected";
        }

        return displayResult(res);
    }

    public String displayShortParamValue(Object x) {
        if (x == null) {
            return "null";
        }

        try {
            return String2DataConvertorFactory.getConvertor(x.getClass()).format(x, null);
        } catch (Throwable t) {
        }

        IOpenClass ioc = null;
        if (x instanceof DynamicObject) {
            ioc = ((DynamicObject) x).getType();
        } else {
            ioc = JavaOpenClass.getOpenClass(x.getClass());
        }

        IOpenField f = ioc.getField("name", true);

        String type = StringTool.lastToken(ioc.getName(), ".");

        Object fvalue = f == null ? null : f.get(x, null);

        return type + (fvalue == null ? "" : String.valueOf(fvalue));

    }

    /**
     * @param tt
     * @return
     */
    private String displayTestResult(TestResult tt) {

        StringBuffer buf = new StringBuffer();
        buf.append("<table class=\"testtable\">\n");
        int nrows = tt.getNumberOfTests();

        buf.append("<tr>");

        String[] hh = tt.getTestHeaders();
        for (int i = 0; i < hh.length; i++) {
            buf.append("<th  class=\"testtable\">").append(hh[i]).append("</th>");
        }
        buf.append("<th class=\"testtable\">").append("Expected").append("</th>");
        buf.append("<th class=\"testtable\">").append("Result").append("</th>");

        buf.append("</tr>\n");

        for (int i = 0; i < nrows; i++) {

            buf.append("<tr>\n");

            for (int j = 0; j < hh.length; j++) {
                buf.append("<td class=\"testtable\">").append(displayResult(tt.getTestValue(hh[j], i))).append("</td>");
            }

            buf.append("<td class=\"testtable\">").append(displayResult(tt.getExpected(i))).append("</td>");

            buf.append("<td  class=\"testtable\">");
            switch (tt.getCompareResult(i)) {
                case TestResult.TR_OK:
                    buf.append("<img src='webresource/images/test_ok.gif'/>").append(displayResult(tt.getResult(i)));
                    break;
                case TestResult.TR_EXCEPTION:
                    // buf.append("<img
                    // src='webresource/images/test_exception.gif'>");
                    displayException((Throwable) tt.getResult(i), buf);
                    break;
                case TestResult.TR_NEQ:
                    buf.append("<img src='webresource/images/test_neq.gif'/>").append(displayResult(tt.getResult(i)));
                    break;
            }

            // .append(displayResult(tt.getResult(i), expl))
            buf.append("</td>");

            buf.append("</tr>\n");

        }

        buf.append("</table>\n");

        return buf.toString();
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
                    StringValue tableName = getTableName(tsn);
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
                tableSearch.setXlsLink((displayResult(getTableName(foundTable))));
                tableSearchList.add(tableSearch);                
            }
        }
        return tableSearchList;
    }

    public StringValue getTableName(TableSyntaxNode tsn) {
        StringValue name = null;
        Property prop = null;
        TableProperties tableProperties = tsn.getTableProperties();
        if (tableProperties != null && (prop = tableProperties.getProperty("name")) != null) {            
            
            name = new StringValue((String)prop.getValue().getValue());
            name.setMetaInfo(prop.getValue().getMetaInfo());
        } else {
            name = tsn.getHeaderLineValue();
        }
        return name;
    }

    IGridFilter makeFilter(int[] rules, DecisionTable dt) {
        IGridRegion[] regions = new IGridRegion[rules.length];

        for (int i = 0; i < rules.length; i++) {
            regions[i] = dt.getRuleTable(rules[i]).getGridTable().getRegion();
        }

        ColorFilterHolder cf = projectModel == null ? new ColorFilterHolder() : projectModel.getFilterHolder();
        return new ColorGridFilter(new RegionGridSelector(regions, true), cf.makeFilter());

        // return ColorGridFilter.makeTransparentFilter(new RegionGridSelector(
        // regions, true), 0.7, 0x00ff00);

        // return new RuleTracerCellFilter(rtt);

    }

    void makeXlsOrDocUrl(String uri, StringBuffer buf) {

        String url = WebTool.makeXlsOrDocUrl(uri);
        buf.append("href='" + WebContext.getContextPath() + "/jsp/showLinks.jsp?").append(url).append("'");
        buf.append(" target='show_app_hidden'");
    }

    public String stringValueof(Object obj) {
        return String.valueOf(obj);
    }

    StringBuffer toHtml(String msg, StringBuffer buf) {
        return StringTool.prepareXMLAttributeValue(msg, buf);
    }

}
