package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.openl.meta.IMetaInfo;
import org.openl.meta.ValueMetaInfo;
import org.openl.meta.explanation.ExplanationNumberValue;
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
    private Map<Integer, Integer> expandLevels = new HashMap<Integer, Integer>();

    public Explanation(ExplanationNumberValue<?> root, String rootID) {
        this.rootID = rootID;
        expandedValues.add(root);
    }

    private boolean isExpanded(ExplanationNumberValue<?> v) {
        for (ExplanationNumberValue value : expandedValues) {
            if (value == v) { // Don't use equals
                return true;
            }
        }
        return false;
    }

    private String expandArgument(ExplanationNumberValue<?> value,
            boolean isMultiplicative,
            String parentUrl,
            int level) {

        if (value == null) {
            return null;
        }

        String url = findUrl(value, parentUrl);
        if (value.isFormula()) {
            if (value.getFormula().isMultiplicative() == isMultiplicative && level < MAX_LEVEL) {
                return expandFormula(value, url, level + 1);
            }
            return expandValue(value);
        } else if (value.isCast()) {
            return expandCast(value, isMultiplicative, url, level);
        } else if (value.isFunction()) {
            return expandValue(value);
        }
        return resultValue(value);
    }

    private String expandFormula(ExplanationNumberValue<?> value, String parentUrl, int level) {
        String url = findUrl(value, parentUrl);

        String arg1 = expandArgument(value.getFormula().getV1(), value.getFormula().isMultiplicative(), url, level);
        String arg2 = expandArgument(value.getFormula().getV2(), value.getFormula().isMultiplicative(), url, level);
        String operand = value.getFormula().getOperand();

        return String.format("%s %s %s", arg1, operand, arg2);
    }

    private String expandFunction(ExplanationNumberValue<?> value) {
        String url = findUrl(value, null);
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

    private String expandCast(ExplanationNumberValue<?> value, boolean isMultiplicative, String parentUrl, int level) {
        String url = findUrl(value, parentUrl);

        CastOperand operand = value.getCast().getOperand();

        String argument = expandArgument(value.getCast().getValue(), operand.isAutocast() && isMultiplicative, url, level);

        return operand.isAutocast() ? argument : "(" + operand.getType() + ")(" + argument + ")";
    }

    private String expandValue(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);
        int id = getUniqueId(explanationValue);

        if (isExpanded(explanationValue)) {
            return "<span class='expanded' data-id='" + id + "'>" + value + "</span>";
        } else {
            String url = "?rootID=" + rootID + "&expandID=" + id + "&from=" + currentId;
            return HTMLHelper.urlLink(url, "Explain", value, null, "explain");
        }
    }

    private String resultValue(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);

        IMetaInfo mi = explanationValue.getMetaInfo();
        String name = mi != null ? mi.getDisplayName(IMetaInfo.LONG) : null;
        String url = findUrl(explanationValue, null);

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

    private String findUrl(ExplanationNumberValue<?> value, String parentUrl) {
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

    private String htmlString(ExplanationNumberValue<?> value) {
        if (value.isFormula()) {
            return expandFormula(value, null, 0);
        } else if (value.isFunction()) {
            return expandFunction(value);
        } else if (value.isCast()) {
            return expandCast(value, false, null, 0);
        }
        return resultValue(value);
    }

    private int currentId;

    private String[] htmlTable(ExplanationNumberValue<?> explanationValue) {
        String value = getFormattedValue(explanationValue);
        int id = getUniqueId(explanationValue);
        currentId = id;

        Integer level = expandLevels.get(id);
        if (level == null) {
            level = 0;
        }

        return new String[] { String.valueOf(id), level.toString(), value, htmlString(explanationValue) };
    }

    public List<String[]> getExplainList(String expandID, String fromID) {
        if (expandID != null) {
            ExplanationNumberValue<?> ev = find(expandID);

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
