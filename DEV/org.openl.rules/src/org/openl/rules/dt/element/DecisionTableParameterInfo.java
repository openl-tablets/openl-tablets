package org.openl.rules.dt.element;

import org.openl.types.IParameterDeclaration;

public class DecisionTableParameterInfo {

    private int index;
    private IDecisionRow row;

    public DecisionTableParameterInfo(int index, IDecisionRow row) {
        this.index = index;
        this.row = row;
    }

    public int getIndex() {
        return index;
    }

    public IParameterDeclaration getParameterDeclaration() {
        return row.getParams()[index];
    }

    public String getPresentation() {
        return row.getParamPresentation()[index];
    }

    public IDecisionRow getRow() {
        return row;
    }

    public Object getValue(int i) {

        Object[][] paramValues = row.getParamValues();

        if (paramValues != null && paramValues[i] != null) {
            return paramValues[i][index];
        }

        return null;
    }

}
