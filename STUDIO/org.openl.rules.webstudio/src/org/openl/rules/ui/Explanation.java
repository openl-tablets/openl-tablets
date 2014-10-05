package org.openl.rules.ui;

import org.openl.meta.IMetaInfo;
import org.openl.meta.ValueMetaInfo;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.NumberCast;
import org.openl.meta.number.NumberFormula;
import org.openl.meta.number.NumberValue.ValueType;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.tableeditor.model.ui.util.HTMLHelper;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.util.AOpenIterator;
import org.openl.util.OpenIterator;
import org.openl.util.StringTool;
import org.openl.util.tree.TreeIterator;

import java.util.*;

public class Explanation {
    static class ExplanationValueIterator implements TreeIterator.TreeAdaptor {

        public Iterator<?> children(Object node) { // node.getType == NumberValueTypes.Formula
            if (node.getClass() == ExplanationNumberValue.class
                    && ((ExplanationNumberValue<?>) node).getValueType().equals(ValueType.FORMULA)) {
                return ((ExplanationNumberValue<?>) node).getFormula().getArguments().iterator();
            } else if (node.getClass() == ExplanationNumberValue.class
                    && ((ExplanationNumberValue<?>) node).getValueType().equals(ValueType.FUNCTION)) {
                return OpenIterator.fromArray(((ExplanationNumberValue<?>) node).getFunction().getParams());
            } else if (node.getClass() == ExplanationNumberValue.class
                    && ((ExplanationNumberValue<?>) node).getValueType().equals(ValueType.CAST)) {
                @SuppressWarnings("unchecked")
                List<?> list = Arrays.asList(((ExplanationNumberValue<?>) node).getCast().getValue());
                return list.iterator();
            } else {
                return AOpenIterator.EMPTY;
            }
        }
    }

    private static final int MAX_LEVEL = 2; // Maximum expansion level for formulas

    private ExplanationNumberValue<?> root;

    private List<ExplanationNumberValue<?>> expandedValues = new ArrayList<ExplanationNumberValue<?>>();
    private Map<Integer, Integer> expandLevels = new HashMap<Integer, Integer>();

    private String header;

    private Explanator explanator;

    public static String getName(ExplanationNumberValue<?> value) {
        IMetaInfo mi = value.getMetaInfo();
        return mi != null ? mi.getDisplayName(IMetaInfo.LONG) : null;
    }

    public Explanation(Explanator explanator) {
        this.explanator = explanator;
    }

    public void expand(String expandID, String fromID) {
        ExplanationNumberValue<?> ev = explanator.find(expandID);

        if (!isExpanded(ev)) {
            expandedValues.add(ev);

            int from = Integer.valueOf(fromID);
            Integer levelFrom = expandLevels.get(from);
            if (levelFrom != null) {
                expandLevels.put(Integer.valueOf(expandID), levelFrom + 1);
            } else {
                expandLevels.put(levelFrom, 0);
                expandLevels.put(Integer.valueOf(expandID), 1);
            }
        }
    }

    public boolean isExpanded(ExplanationNumberValue<?> v) {
        for (ExplanationNumberValue value : expandedValues) {
            if (value == v) { // Don't use equals
                return true;
            }
        }
        return false;
    }

    protected String expandArgument(ExplanationNumberValue<?> value, boolean isMultiplicative, String parentUrl,
                                    int level) {

        if (value == null) {
            return null;
        }

        String url = findUrl(value, parentUrl);
        if (value.getValueType().equals(ValueType.FORMULA)) {
            NumberFormula<?> formula = value.getFormula();
            if (formula.isMultiplicative() == isMultiplicative && level < MAX_LEVEL) {
                return expandFormula(value, url, level + 1);
            }

        } else if (value.getValueType().equals(ValueType.CAST)) {
            return expandCast(value, isMultiplicative, url, level);
        }

        return isExpandable(value) ? expandValue(value) : resultValue(value);
    }

    protected String expandFormula(ExplanationNumberValue<?> value, String parentUrl, int level) {
        String url = findUrl(value, parentUrl);

        String arg1 = expandArgument(value.getFormula().getV1(), value.getFormula().isMultiplicative(), url, level);
        String arg2 = expandArgument(value.getFormula().getV2(), value.getFormula().isMultiplicative(), url, level);
        String operand = value.getFormula().getOperand();

        return String.format("%s %s %s", arg1, operand, arg2);
    }

    private String expandFunction(ExplanationNumberValue<?> value, String parentUrl) {
        String url = findUrl(value, parentUrl);
        StringBuilder ret = new StringBuilder(value.getFunction().getFunctionName().toUpperCase()).append(" (");
        ExplanationNumberValue<?>[] params = value.getFunction().getParams();

        for (int i = 0; params != null && i < params.length; i++) {
            if (i > 0) {
                ret.append(", ");
            }
            ret.append(expandArgument(params[i], true, url, 0));
        }
        return ret.append(")").toString();
    }

    protected String expandCast(ExplanationNumberValue<?> value, boolean isMultiplicative, String parentUrl, int level) {
        String url = findUrl(value, parentUrl);

        NumberCast cast = value.getCast();
        CastOperand operand = cast.getOperand();

        String argument = expandArgument(cast.getValue(), operand.isAutocast() && isMultiplicative, url, level);

        return operand.isAutocast() ? argument : "(" + operand.getType() + ")(" + argument + ")";
    }

    public String expandValue(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);
        int id = explanator.getUniqueId(explanationValue);

        if (isExpanded(explanationValue)) {
            return "<span class='expanded' data-id='" + id + "'>" + value + "</span>";
        } else {
            return HTMLHelper.urlLink(makeExpandUrl(id), "Explain", value, null, "explain");
        }
    }

    private String resultValue(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);

        String name = getName(explanationValue);
        String url = findUrl(explanationValue, null);

        if (url != null && name != null) {
            value = HTMLHelper.urlLink(
                    WebContext.getContextPath() + "/faces/pages/modules/explain/showExplainTable.xhtml?uri="
                            + StringTool.encodeURL(url) + "&text=" + name,
                    "Show in table", value, "mainFrame", "open");
        }

        return value;
    }

    private String getFormattedValue(ExplanationNumberValue<?> explanationValue) {
        return FormattersManager.format(explanationValue);
    }

    public String findUrl(ExplanationNumberValue<?> value, String parentUrl) {
        String url = null;

        ValueMetaInfo mi = (ValueMetaInfo) value.getMetaInfo();
        if (mi != null) {
            // Get cell uri
            url = mi.getSourceUrl();
        }

        if (url == null) {
            url = parentUrl;
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

        } else if (ValueType.CAST.equals(value.getValueType())) {
            return expandCast(value, false, null, 0);
        }

        return resultValue(value);
    }

    private int currentId;

    public String[] htmlTable(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);
        int id = explanator.getUniqueId(explanationValue);
        currentId = id;

        Integer level = expandLevels.get(id);
        if (level == null) {
            level = 0;
        }

        return new String[]{String.valueOf(id), level.toString(), value, htmlString(explanationValue)};
    }

    protected boolean isExpandable(ExplanationNumberValue<?> value) {
        switch (value.getValueType()) {
            case FORMULA:
            case FUNCTION:
            case CAST:
                return true;
            default:
                return false;
        }
    }

    protected String makeBasicUrl() {
        return "?rootID=" + explanator.getUniqueId(root) + "&header=" + header;
    }

    protected String makeExpandUrl(int id) {
        return makeBasicUrl() + "&expandID=" + id + "&from=" + currentId;
    }

    public void setExpandedValues(List<ExplanationNumberValue<?>> expandedValues) {
        this.expandedValues = expandedValues;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public ExplanationNumberValue<?> getRoot() {
        return root;
    }

    public void setRoot(ExplanationNumberValue<?> root) {
        this.root = root;
    }

}
