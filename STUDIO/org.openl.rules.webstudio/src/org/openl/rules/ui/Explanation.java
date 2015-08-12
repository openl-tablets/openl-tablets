package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.openl.meta.IMetaInfo;
import org.openl.meta.explanation.CastExplanationValue;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.explanation.FormulaExplanationValue;
import org.openl.meta.number.CastOperand;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.tableeditor.model.ui.util.HTMLHelper;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.util.StringTool;

public class Explanation {

    private static final int MAX_LEVEL = 2; // Maximum expansion level for
                                            // formulas

    private String rootID;

    private List<ExplanationNumberValue<?>> expandedValues = new ArrayList<ExplanationNumberValue<?>>();
    private Map<ExplanationNumberValue<?>, Integer> expandLevels = new IdentityHashMap<ExplanationNumberValue<?>, Integer>();

    public Explanation(ExplanationNumberValue<?> root, String rootID) {
        this.rootID = rootID;
        expandedValues.add(root);
        expandLevels.put(root, 0);
    }

    private boolean isExpanded(ExplanationNumberValue<?> v) {
        for (ExplanationNumberValue value : expandedValues) {
            if (value == v) { // Don't use equals
                return true;
            }
        }
        return false;
    }

    private String expandArgument(ExplanationNumberValue<?> value, boolean isMultiplicative, int level, Integer indent) {

        if (value == null) {
            return null;
        }

        if (!value.isFunction() && !value.isFormula() && !value.isCast()) {
            return resultValue(value);
        }

        if (value.isCast() && value.getCast().getOperand().isAutocast()) {
            return expandCast(value, level, indent);
        }

        if (value.isFunction() || value.isCast() || level >= MAX_LEVEL || (value.isFormula() && value.getFormula().isMultiplicative() != isMultiplicative)) {
            expandLevels.put(value, indent + 1);
            return expandValue(value);
        }

        return expandFormula(value, level + 1, indent);
    }

    private String expandFormula(ExplanationNumberValue<?> value, int level, Integer indent) {

        FormulaExplanationValue<? extends ExplanationNumberValue<?>> formula = value.getFormula();
        String arg1 = expandArgument(formula.getV1(), formula.isMultiplicative(), level, indent);
        String arg2 = expandArgument(formula.getV2(), formula.isMultiplicative(), level, indent);
        String operand = formula.getOperand();
        formula.isMultiplicative();

        return String.format("%s %s %s", arg1, operand, arg2);
    }

    private String expandFunction(ExplanationNumberValue<?> value, Integer indent) {
        StringBuilder ret = new StringBuilder(value.getFunction().getFunctionName().toUpperCase()).append(" (");
        ExplanationNumberValue<?>[] params = value.getFunction().getParams();

        for (int i = 0; params != null && i < params.length; i++) {
            if (i > 0) {
                ret.append(", ");
            }
            ExplanationNumberValue<?> param = params[i];
            ret.append(expandArgument(param, false, MAX_LEVEL, indent));
        }
        return ret.append(")").toString();
    }

    private String expandCast(ExplanationNumberValue<?> value, int level, Integer indent) {
        CastExplanationValue castExplanation = value.getCast();
        CastOperand operand = castExplanation.getOperand();
        boolean autocast = operand.isAutocast();

        String argument = expandArgument(castExplanation.getValue(), autocast, level, indent);

        return autocast ? argument : "(" + operand.getType() + ")(" + argument + ")";
    }

    private String expandValue(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);
        int id = getUniqueId(explanationValue);

        if (isExpanded(explanationValue)) {
            return "<span class='expanded' data-id='" + id + "'>" + value + "</span>";
        } else {
            String url = "?rootID=" + rootID + "&expandID=" + id;
            return HTMLHelper.urlLink(url, "Explain", value, null, "explain");
        }
    }

    private String resultValue(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);

        IMetaInfo mi = explanationValue.getMetaInfo();
        String name = mi != null ? mi.getDisplayName(IMetaInfo.LONG) : null;
        String url = mi != null ? mi.getSourceUrl() : null;

        if (url != null && name != null) {
            value = HTMLHelper.urlLink(WebContext.getContextPath() + "/faces/pages/modules/explain/showExplainTable.xhtml?uri=" + StringTool.encodeURL(url) + "&text=" + name,
                "Show in table",
                value,
                "mainFrame",
                "open");
        }

        return value;
    }

    private String getFormattedValue(ExplanationNumberValue<?> explanationValue) {
        return FormattersManager.format(explanationValue);
    }

    private String htmlString(ExplanationNumberValue<?> value, Integer indent) {
        if (value.isFormula()) {
            return expandFormula(value, 0, indent);
        } else if (value.isFunction()) {
            return expandFunction(value, indent);
        } else if (value.isCast()) {
            return expandCast(value, 0, indent);
        }
        return resultValue(value);
    }

    private String[] htmlTable(ExplanationNumberValue<?> explanationValue) {
        int id = getUniqueId(explanationValue);
        Integer indent = expandLevels.get(explanationValue);
        String value = getFormattedValue(explanationValue);
        String html = htmlString(explanationValue, indent);

        return new String[] { String.valueOf(id), indent.toString(), value, html };
    }

    public List<String[]> getExplainList(String expandID) {
        if (expandID != null) {
            ExplanationNumberValue<?> ev = find(expandID);

            if (!isExpanded(ev)) {
                expandedValues.add(ev);
            }
        }
        List<String[]> expandedValuesList = new ArrayList<String[]>();

        for (ExplanationNumberValue<?> explanationValue : expandedValues) {
            String[] html = htmlTable(explanationValue);
            expandedValuesList.add(html);
        }

        return expandedValuesList;

    }

    private int uniqueId = 0;

    private IdentityHashMap<ExplanationNumberValue<?>, Integer> value2id = new IdentityHashMap<ExplanationNumberValue<?>, Integer>();

    private Map<Integer, ExplanationNumberValue<?>> id2value = new HashMap<Integer, ExplanationNumberValue<?>>();

    private ExplanationNumberValue<?> find(String expandID) {
        return id2value.get(Integer.parseInt(expandID));
    }

    private int getUniqueId(ExplanationNumberValue<?> value) {
        Integer id = value2id.get(value);

        if (id != null) {
            return id;
        }

        id = ++uniqueId;
        value2id.put(value, id);
        id2value.put(id, value);
        return id;
    }
}
