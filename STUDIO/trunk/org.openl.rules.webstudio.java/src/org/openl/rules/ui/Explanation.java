package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaInfo;
import org.openl.meta.number.NumberFormula;
import org.openl.meta.number.NumberValue.ValueType;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.tableeditor.model.ui.util.HTMLHelper;
import org.openl.util.AOpenIterator;
import org.openl.util.OpenIterator;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;
import org.openl.util.tree.TreeIterator;

public class Explanation {
    static class DoubleValueIterator implements TreeIterator.TreeAdaptor {

        public Iterator children(Object node) { // node.getType == NumberValueTypes.Formula
            if (node.getClass() == DoubleValue.class && ((DoubleValue)node).getValueType().equals(ValueType.FORMULA)) {
                return OpenIterator.fromArray(((DoubleValue) node).getFormula().getArguments());
            } else if (node.getClass() == DoubleValue.class && 
                    ((DoubleValue)node).getValueType().equals(ValueType.FUNCTION)) {
                return OpenIterator.fromArray(((DoubleValue) node).getFunction().getParams());
            } else {
                return AOpenIterator.EMPTY;
            }
        }
    }

    static final int MAX_LEVEL = 2; // Maximum expansion level for formulas

    // String pname;
    // String period;

    DoubleValue root;

    List<DoubleValue> expandedValues = new ArrayList<DoubleValue>();
    String header;

    boolean showNamesInFormula = false;

    boolean showValuesInFormula = true;

    Explanator explanator;

    static String getName(DoubleValue value) {
        IMetaInfo mi = value.getMetaInfo();
        String name = mi != null ? mi.getDisplayName(IMetaInfo.LONG) : null;
        return name;
    }

    public Explanation(Explanator explanator) {
        this.explanator = explanator;
    }

    public void expand(String expandID) {
        DoubleValue dv = explanator.find(expandID);
        if (!expandedValues.contains(dv)) {
            expandedValues.add(dv);
        }
    }

    protected String expandArgument(DoubleValue value, boolean isMultiplicative, String parentUrl, int level) {
        String url = findUrl(value, parentUrl);
        if (value.getValueType().equals(ValueType.FORMULA)) {
            NumberFormula<DoubleValue> formula = value.getFormula();
            if (formula.isMultiplicative() == isMultiplicative && level < MAX_LEVEL) {
                return expandFormula(value, url, level + 1);
            }
        }

        return expandValue(value);

    }

    protected String expandFormula(DoubleValue value, String parentUrl, int level) {

        String url = findUrl(value, parentUrl);
        return expandArgument(value.getFormula().getV1(), value.getFormula().isMultiplicative(), url, level) + value.getFormula().getOperand()
                + expandArgument(value.getFormula().getV2(), value.getFormula().isMultiplicative(), url, level);
    }

    private String expandFunction(DoubleValue value, String parentUrl) {
        String url = findUrl(value, parentUrl);
        String ret = value.getFunction().getFunctionName() + "(";
        DoubleValue[] params = value.getFunction().getParams();

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                ret += ", ";
            }
            ret += expandArgument(params[i], false, url, 0);
        }
        return ret + ")";
    }

    public String expandValue(DoubleValue value) {
        String text = String.valueOf(value.getValue());

        String name = getName(value);

        if (name != null && showNamesInFormula) {
            if (showValuesInFormula) {
                text = name + "(" + text + ")";
            } else {
                text = name;
            }
        }

        if (expandedValues.contains(value)) {
            return text;
        }
        int id = explanator.getUniqueId(value);

        return HTMLHelper.urlLink(makeExpandUrl(id), name == null ? "expand" : name, text, null);
    }

    public String findUrl(DoubleValue value, String parentUrl) {
        IMetaInfo mi = value.getMetaInfo();

        String url = mi != null ? mi.getSourceUrl() : null;
        if (url == null) {
            return parentUrl;
        }
        return url;

    }

    public List<DoubleValue> getExpandedValues() {
        return expandedValues;
    }

    public String getHeader() {
        return header;
    }

    public DoubleValue getExplainTree() {
        return root;
    }

    public String htmlString(DoubleValue value) {
        if (ValueType.FORMULA.equals(value.getValueType())) {
            return expandFormula(value, null, 0);
        } else if (ValueType.FUNCTION.equals(value.getValueType())) {
            return expandFunction(value, null);
        }
        return expandValue(value);
    }

    public String[] htmlTable(DoubleValue value) {
        String text = String.valueOf(value.getValue());
        String url = findUrl(value, null);
        IMetaInfo mi = value.getMetaInfo();
        String name = mi != null ? mi.getDisplayName(IMetaInfo.LONG) : null;

        if (url != null) {
            text = HTMLHelper.urlLink(makeUrl(url), "show", text, null);
        }

        if (name == null) {
            name = "";
        } else if (url != null) {
            name = HTMLHelper.urlLink("showExplainTable.jsp?uri=" + StringTool.encodeURL(url) + "&text=" + name, "show",
                    name, "mainFrame");
        }

        return new String[] { text, name, htmlString(value) };
    }

    protected boolean isExpandable(DoubleValue value) {
        return value.getValueType().equals(ValueType.FORMULA) || value.getValueType().equals(ValueType.FUNCTION);
    }

    public boolean isShowNamesInFormula() {
        return showNamesInFormula;
    }

    public boolean isShowValuesInFormula() {
        return showValuesInFormula;
    }

    protected String makeBasicUrl() {
        // return "explain.jsp?rootID=" + explanator.getUniqueId(root)
        return "?rootID=" + explanator.getUniqueId(root) + "&header=" + header
        // + "&pname=" + pname
                // + "&period=" + period
                + (showNamesInFormula ? "&showNames=true" : "") + (showValuesInFormula ? "&showValues=true" : "");
    }

    protected String makeExpandUrl(int id) {
        return makeBasicUrl() + "&expandID=" + id;
    }

    // public String getPeriod()
    // {
    // return period;
    // }
    //
    // public void setPeriod(String period)
    // {
    // this.period = period;
    // }
    //
    // public String getPname()
    // {
    // return pname;
    // }
    //
    // public void setPname(String pname)
    // {
    // this.pname = pname;
    // }

    protected String makeUrl(String url) {
        if (url == null) {
            return "#";
        }

        XlsUrlParser parser = new XlsUrlParser();
        try {
            parser.parse(url);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

        String ret = makeBasicUrl() + "&wbPath=" + parser.wbPath + "&wbName=" + parser.wbName + "&wsName="
                + parser.wsName + "&range=" + parser.range;

        return ret;
    }

    public void setExpandedValues(List<DoubleValue> expandedValues) {
        this.expandedValues = expandedValues;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setShowNamesInFormula(boolean showNamesInFormula) {
        this.showNamesInFormula = showNamesInFormula;
    }

    public void setShowValuesInFormula(boolean showValuesInFormula) {
        this.showValuesInFormula = showValuesInFormula;
    }
}
