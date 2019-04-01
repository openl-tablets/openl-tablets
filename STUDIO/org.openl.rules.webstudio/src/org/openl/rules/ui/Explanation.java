package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.List;

import org.openl.meta.IMetaInfo;
import org.openl.meta.explanation.CastExplanationValue;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.explanation.FormulaExplanationValue;
import org.openl.meta.number.CastOperand;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.util.StringTool;

public class Explanation {

    private static final int MAX_LEVEL = 2; // Maximum expansion level for formulas

    private String expandArgument(ExplanationNumberValue<?> value,
            boolean isMultiplicative,
            int level,
            Integer indent) {

        if (value == null) {
            return null;
        }

        if (!value.isFunction() && !value.isFormula() && !value.isCast()) {
            return resultValue(value);
        }

        if (value.isCast() && value.getCast().getOperand().isAutocast()) {
            return expandCast(value, level, indent);
        }

        if (value.isFunction() || value.isCast() || level >= MAX_LEVEL || (value
            .isFormula() && value.getFormula().isMultiplicative() != isMultiplicative)) {
            return explain(value, indent);
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

    private String resultValue(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);

        IMetaInfo mi = explanationValue.getMetaInfo();
        String name = mi != null ? mi.getDisplayName(IMetaInfo.LONG) : null;
        String url = mi != null ? mi.getSourceUrl() : null;

        if (url != null && name != null) {
            String href = WebContext
                .getContextPath() + "/faces/pages/modules/explain/showExplainTable.xhtml?uri=" + StringTool
                    .encodeURL(url) + "&text=" + name;
            value = "<a href='" + href + "' title='Show in table' target='mainFrame' class='open'>" + value + "</a>";
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

    private List<String[]> result = new ArrayList<>();
    private int counter = 0;

    private String explain(ExplanationNumberValue<?> explanationValue, int level) {
        String id = "expl" + (counter++);
        String formatted = getFormattedValue(explanationValue);
        String[] item = new String[4];
        result.add(item);
        item[0] = id;
        item[1] = String.valueOf(level);
        item[2] = formatted;
        item[3] = htmlString(explanationValue, level + 1);
        return "<span class='explain' data-id='" + id + "'>" + formatted + "</span>";
    }

    public List<String[]> build(ExplanationNumberValue<?> root) {
        result = new ArrayList<>();
        explain(root, 0);
        return result;
    }
}
