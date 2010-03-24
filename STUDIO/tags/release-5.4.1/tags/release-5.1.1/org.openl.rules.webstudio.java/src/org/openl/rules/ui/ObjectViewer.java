/**
 * Created Jan 5, 2007
 */
package org.openl.rules.ui;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.IOpenSourceCodeModule;
import org.openl.base.INamedThing;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.BoundError;
import org.openl.main.SourceCodeURLTool;
import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaHolder;
import org.openl.meta.OpenLRuntimeExceptionWithMetaInfo;
import org.openl.meta.StringValue;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.rules.dt.DTOverlapping;
import org.openl.rules.dt.DTRule;
import org.openl.rules.dt.DTUncovered;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDecisionTableConstants;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.search.ISearchTableRow;
import org.openl.rules.search.OpenLAdvancedSearchResult;
import org.openl.rules.search.OpenLAdvancedSearchResultViewer;
import org.openl.rules.search.OpenLAdvancedSearchResult.TableAndRows;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ui.ColorGridFilter;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.testmethod.TestResult;
import org.openl.rules.validator.dt.DTValidationResult;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.webstudio.web.tableeditor.TableRenderer;
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

/**
 * @author snshor
 */
public class ObjectViewer {
    private ProjectModel projectModel;

    public ObjectViewer(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    //    public ObjectViewer() {}

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
        if (el == null)
            return "[]";

        if (el.getClass().isArray())
            return displayMatrix(res);

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < len; i++) {
            buf.append("<p>\n");
            buf.append(displayResult(Array.get(res, i)));
        }

        return buf.toString();
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
            if (row == null)
                continue;

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

    public String displayDoubleValueWithExplanation(DoubleValue dv, NumberFormat format, String clazz) {
        return "<a href=\"" + getURL(dv) + "\"" + (clazz == null ? "" : " class=\"" + clazz + "\"") + ">"
        // + new
                // String2DataConvertorFactory.String2DoubleConvertor().format(dv,dv.getFormat())
                + format.format(dv) + "</a>";
    }

    public String getURL(DoubleValue dv) {
        int rootID = Explanator.getCurrent().getUniqueId(dv);
        return "javascript: open_explain_win(\'?rootID=" + rootID + "&header=Explanation')";
    }

    public String displayDoubleValueWithExplanation(DoubleValue dv) {
        return displayDoubleValueWithExplanation(dv, format, null);
    }

    public String displayDoubleValueWithExplanation(DoubleValue dv, String clazz) {
        return displayDoubleValueWithExplanation(dv, format, clazz);
    }

    static NumberFormat format = new DecimalFormat("#.0#");

    public String displayResult(Object res) {
        if (res == null)
            return "<b>null</b>";

        if (res.getClass().equals(new DTRule[0].getClass()))
            return displayRuleArray((DTRule[]) res);

        if (res.getClass().isArray())
            return displayArray(res);

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
                    TableRenderer renderer = new TableRenderer(tableModel);
                    renderer.setCellIdPrefix("cell-" + index + "-");
                    return renderer.renderWithMenu();
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

        String value = res.toString();

        return value;
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

            StringValue tname = null;
            Property prop = null;
            if (tsn.getTableProperties() != null && (prop = tsn.getTableProperties().getProperty("name")) != null) {
                tname = prop.getValue();
            } else {
                tname = tr[i].getTsn().getHeaderLineValue();
            }

            res[2 * i] = tname;

            ISearchTableRow[] trows = tr[i].getRows();

            CompositeGrid cg = sviewer.makeGrid(trows);

            res[2 * i + 1] = cg != null ? new GridWithNode(cg.asGridTable(), tsn) : "No rows selected";

        }

        return displayResult(res);
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

        //		makeExcelLink(gt, "Show Table In Excel", buf);

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

            String type = IDecisionTableConstants.VIEW_BUSINESS;
            ILogicalTable gtx = (ILogicalTable) tsn.getSubTables().get(type);
            if (gtx != null)
                gt = gtx.getGridTable();

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
            ILogicalTable gtx = (ILogicalTable) tsn.getSubTables().get(type);
            if (gtx != null)
                gt = gtx.getGridTable();

            buf.append(ProjectModel.showTable(gt, false));

        }
        return buf.toString();
    }

    IGridFilter makeFilter(int[] rules, DecisionTable dt) {
        IGridRegion[] regions = new IGridRegion[rules.length];

        for (int i = 0; i < rules.length; i++) {
            regions[i] = dt.getRuleTable(rules[i]).getGridTable().getRegion();
        }

        ColorFilterHolder cf = projectModel == null ? new ColorFilterHolder() : projectModel.getFilterHolder();
        return new ColorGridFilter(new RegionGridSelector(regions, true), cf.makeFilter());

        //		return ColorGridFilter.makeTransparentFilter(new RegionGridSelector(
        //				regions, true), 0.7, 0x00ff00);

        // return new RuleTracerCellFilter(rtt);

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
                // buf.append("<img src='webresource/images/test_exception.gif'>");
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

    private StringBuffer displayNonOpenlException(Throwable t, StringBuffer buf) {
        buf.append("<span class=\"codeerror\"><b>");
        buf.append(t.getClass().getName()).append(": ").append("</b></span>");

        toHtml(t.getMessage(), buf);
        return buf;
    }

    StringBuffer toHtml(String msg, StringBuffer buf) {
        return StringTool.prepareXMLAttributeValue(msg, buf);
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
                        || tt[i] instanceof BoundError || tt[i] instanceof OpenLRuntimeExceptionWithMetaInfo)
                    return displayException(tt[i], buf);
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

    public StringBuffer displayMetaHolder(IMetaHolder mh, String display, StringBuffer buf) {
        buf.append("<a ");
        makeXlsOrDocUrl(mh.getMetaInfo().getSourceUrl(), buf);
        buf.append(">");
        StringTool.encodeHTMLBody(display, buf);
        buf.append("</a>");

        return buf;

    }

    public String stringValueof(Object obj) {
        return String.valueOf(obj);
    }

    static public StringBuffer printCodeAndErrorToHtml(ILocation location, IOpenSourceCodeModule module,
            StringBuffer buf) {

        String src = null;
        int pstart = 0;
        int pend = 0;
        if (location == null || !location.isTextLocation()) {

            try {
                src = module.getCode();
            } catch (NullPointerException e) {
            } catch (UnsupportedOperationException e) {
            }
            if (src == null || src.trim().length() == 0)
                src = "View Table in Excel";
            pstart = 0;
            pend = src.length();
        } else {
            src = module.getCode();
            TextInfo info = new TextInfo(src);
            pstart = location.getStart().getAbsolutePosition(info);
            pend = Math.min(location.getEnd().getAbsolutePosition(info) + 1, src.length());
        }

        buf.append("\n<pre>\n");
        buf.append(src.substring(0, pstart));

        buf.append("<span class='codeerror'>");
        buf.append(src.substring(pstart, pend));
        buf.append("</span>");
        if (pend < src.length())
            ;
        buf.append(src.substring(pend, src.length()));

        buf.append("</pre>\n");

        return buf;

    }

    void makeXlsOrDocUrl(String uri, StringBuffer buf) {

        String url = WebTool.makeXlsOrDocUrl(uri);
        buf.append("href=\"/webstudio/jsp/showLinks.jsp?").append(url).append('"');
        buf.append(" target='show_app_hidden'");
    }

    private void displayErrorAndCode(Throwable tt, ILocation srcLocation, IOpenSourceCodeModule module, StringBuffer buf) {
        buf.append("<table><tr><td class='error_box'>");
        displayNonOpenlException(tt, buf).append("<p/>\n");
        String uri = SourceCodeURLTool.makeSourceLocationURL(srcLocation, module, "");
        String url = uri == null || "NO_MODULE".equals(uri) ? "NO+URL" : WebTool.makeXlsOrDocUrl(uri);

        buf.append("\n" + "<table><tr><td class='javacode'>");

        buf.append("<a class='left' href='javascript:errorClicked(\"").append(url).append(
                "\")' title='Edit cell containing error'>");

        printCodeAndErrorToHtml(srcLocation, module, buf);
        buf.append("</a>");

        buf.append("<a href='../showLinks.jsp?").append(url).append("' target='show_app_hidden' title='").append(uri)
                .append("'> Edit in Excel </a>");
        buf.append("</td></tr></table>");
        buf.append("</td></tr></table>");
    }

    public String displayShortParamValue(Object x) {
        if (x == null)
            return "null";

        try {
            return String2DataConvertorFactory.getConvertor(x.getClass()).format(x, null);
        } catch (Throwable t) {
        }

        IOpenClass ioc = null;
        if (x instanceof DynamicObject) {
            ioc = ((DynamicObject) x).getType();
        } else
            ioc = JavaOpenClass.getOpenClass(x.getClass());

        IOpenField f = ioc.getField("name", true);

        String type = StringTool.lastToken(ioc.getName(), ".");

        Object fvalue = f == null ? null : f.get(x, null);

        return type + (fvalue == null ? "" : String.valueOf(fvalue));

    }

    public static StringBuffer makeExcelLink(IGridTable table, String text, StringBuffer buf) {
        String uri = table.getUri();
        String url = WebTool.makeXlsOrDocUrl(table.getUri());

        buf.append("<img src='webresource/images/excel-workbook.png'/>");
        buf.append("<a class='left' href='showLinks.jsp?" + url + "' target='show_app_hidden' title='" + uri + "'>"
                + "&nbsp;" + text + "</a>");

        return buf;

    }

    private static class GridWithNode {
        IGridTable gridTable;
        TableSyntaxNode tableSyntaxNode;

        private GridWithNode(IGridTable gridTable, TableSyntaxNode tableSyntaxNode) {
            this.gridTable = gridTable;
            this.tableSyntaxNode = tableSyntaxNode;
        }
    }

}
