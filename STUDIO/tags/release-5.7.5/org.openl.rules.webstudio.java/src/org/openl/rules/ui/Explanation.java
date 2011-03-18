package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.meta.IMetaInfo;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberFormula;
import org.openl.meta.number.NumberValue.ValueType;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.tableeditor.model.ui.util.HTMLHelper;
import org.openl.util.AOpenIterator;
import org.openl.util.OpenIterator;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;
import org.openl.util.formatters.IFormatter;
import org.openl.util.tree.TreeIterator;

public class Explanation {
    static class ExplanationValueIterator implements TreeIterator.TreeAdaptor {

        public Iterator<?> children(Object node) { // node.getType == NumberValueTypes.Formula
            if (node.getClass() == ExplanationNumberValue.class && ((ExplanationNumberValue<?>)node).getValueType().equals(ValueType.FORMULA)) {
                return ((ExplanationNumberValue<?>) node).getFormula().getArguments().iterator();
            } else if (node.getClass() == ExplanationNumberValue.class && 
                    ((ExplanationNumberValue<?>)node).getValueType().equals(ValueType.FUNCTION)) {
                return OpenIterator.fromArray(((ExplanationNumberValue<?>) node).getFunction().getParams());
            } else {
                return AOpenIterator.EMPTY;
            }
        }
    }

    private static final int MAX_LEVEL = 2; // Maximum expansion level for formulas

    private ExplanationNumberValue<?> root;

    private List<ExplanationNumberValue<?>> expandedValues = new ArrayList<ExplanationNumberValue<?>>();
    
    private String header;

    private boolean showNamesInFormula = false;

    private boolean showValuesInFormula = true;

    private Explanator explanator;
    
    public static String getName(ExplanationNumberValue<?> value) {
        IMetaInfo mi = value.getMetaInfo();
        String name = mi != null ? mi.getDisplayName(IMetaInfo.LONG) : null;
        return name;
    }

    public Explanation(Explanator explanator) {
        this.explanator = explanator;
    }

    public void expand(String expandID) {
        ExplanationNumberValue<?> ev = explanator.find(expandID);
        if (!expandedValues.contains(ev)) {
            expandedValues.add(ev);
        }
    }
    
    protected String expandArgument(ExplanationNumberValue<?> value, boolean isMultiplicative, String parentUrl, int level) {
        String url = findUrl(value, parentUrl);
        if (value.getValueType().equals(ValueType.FORMULA)) {
            NumberFormula<?> formula = value.getFormula();
            if (formula.isMultiplicative() == isMultiplicative && level < MAX_LEVEL) {
                return expandFormula(value, url, level + 1);
            }
        }

        return expandValue(value);

    }
    
    protected String expandFormula(ExplanationNumberValue<?> value, String parentUrl, int level) {

        String url = findUrl(value, parentUrl);
        return expandArgument(value.getFormula().getV1(), value.getFormula().isMultiplicative(), url, level) + value.getFormula().getOperand()
                + expandArgument(value.getFormula().getV2(), value.getFormula().isMultiplicative(), url, level);
    }
    
    private String expandFunction(ExplanationNumberValue<?> value, String parentUrl) {
        String url = findUrl(value, parentUrl);
        String ret = value.getFunction().getFunctionName() + "(";
        ExplanationNumberValue<?>[] params = value.getFunction().getParams();

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                ret += ", ";
            }
            ret += expandArgument(params[i], false, url, 0);
        }
        return ret + ")";
    }
    
    public String expandValue(ExplanationNumberValue<?> explanationValue) {        
        String value = getFormattedValue(explanationValue);

        String name = getName(explanationValue);

        if (name != null && showNamesInFormula) {
            if (showValuesInFormula) {
                value = name + "(" + value + ")";
            } else {
                value = name;
            }
        }

        if (expandedValues.contains(explanationValue)) {
            return value;
        }
        int id = explanator.getUniqueId(explanationValue);

        return HTMLHelper.urlLink(makeExpandUrl(id), name == null ? "expand" : name, value, null);
    }

    private String getFormattedValue(ExplanationNumberValue<?> explanationValue) {
        IFormatter formatter = FormattersManager.getFormatter(explanationValue);
        String value = null;
        if (formatter != null) {
            value = formatter.format(explanationValue);
        } else {
            value = String.valueOf(explanationValue.toString());
        }
        return value;
    }
    
    public String findUrl(ExplanationNumberValue<?> value, String parentUrl) {
        IMetaInfo mi = value.getMetaInfo();

        String url = mi != null ? mi.getSourceUrl() : null;
        if (url == null) {
            return parentUrl;
        }
        return url;

    }
    
    public List<ExplanationNumberValue<?>> getExpandedValues() {
        return expandedValues;
    }

    public String getHeader() {
        return header;
    }
    
    public ExplanationNumberValue<?> getExplainTree() {
        return root;
    }    
    
    public String htmlString(ExplanationNumberValue<?> value) {
        if (ValueType.FORMULA.equals(value.getValueType())) {
            return expandFormula(value, null, 0);
        } else if (ValueType.FUNCTION.equals(value.getValueType())) {
            return expandFunction(value, null);
        }
        return expandValue(value);
    }
    
    public String[] htmlTable(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);
        String url = findUrl(explanationValue, null);
        IMetaInfo mi = explanationValue.getMetaInfo();
        String name = mi != null ? mi.getDisplayName(IMetaInfo.LONG) : null;

        if (url != null) {
            value = HTMLHelper.urlLink(makeUrl(url), "show", value, null);
        }

        if (name == null) {
            name = "";
        } else if (url != null) {
            name = HTMLHelper.urlLink("showExplainTable.jsp?uri=" + StringTool.encodeURL(url) + "&text=" + name, "show",
                    name, "mainFrame");
        }

        return new String[] { value, name, htmlString(explanationValue) };
    }
    
    protected boolean isExpandable(ExplanationNumberValue<?> value) {
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
    
    public void setExpandedValues(List<ExplanationNumberValue<?>> expandedValues) {
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

    public ExplanationNumberValue<?> getRoot() {
        return root;
    }

    public void setRoot(ExplanationNumberValue<?> root) {
        this.root = root;
    }
    
}
