package org.openl.rules.ui.tablewizard;

import javax.faces.model.SelectItem;

/**
 * @author Aliaksandr Antonik.
 */
public class ConditionClause {
    private final TableCondition tableCondition;
    private String paramName;
    private long variantId;

    ConditionClause(TableCondition tableCondition) {
        this.tableCondition = tableCondition;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public long getVariantId() {
        return variantId;
    }

    public void setVariantId(long variantId) {
        this.variantId = variantId;
    }

    public String getType() {
        Parameter p = tableCondition.getParameterByName(paramName);
        return p == null ? null : p.getType();
    }

    public SelectItem[] getVariantOptions() {
        SelectItem[] ret = null;

        String type = getType();
        if (type != null)
            ret = ConditionClauseRegistry.getInstance().getItemsByType(type);

        return ret == null ? new SelectItem[0] : ret; 
    }
}
