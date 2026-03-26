package org.openl.rules.dt.data;

import org.openl.OpenL;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.dt.DTColumnsDefinitionField;
import org.openl.types.IOpenField;

public class DecisionExprFieldDataType extends ComponentOpenClass {
    private final DecisionTableDataType decisionTableDataType;

    private boolean exprParameterFieldIsUsed = false;

    DecisionExprFieldDataType(DecisionTableDataType decisionTableDataType, OpenL openl) {
        super(DecisionExprFieldDataType.class.getSimpleName(), openl);
        this.decisionTableDataType = decisionTableDataType;
    }

    public boolean isExprParameterFieldIsUsed() {
        return exprParameterFieldIsUsed;
    }

    @Override
    public IOpenField getField(String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField openField = decisionTableDataType.getField(name, strictMatch);
        if (openField instanceof DecisionRowField decisionRowField) {
            exprParameterFieldIsUsed = true;
            return new ExprDecisionRowField(decisionRowField, getOpenl());
        } else if (openField instanceof ConditionOrActionDirectParameterField field2) {
            exprParameterFieldIsUsed = true;
            return new ExprParameterField(field2);
        } else if (openField instanceof DTColumnsDefinitionField field1) {
            return new ExprParameterDTColumnsDefinitionField(field1);
        } else if (openField == null && !name.startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN)) {
            openField = decisionTableDataType.getField(SpreadsheetStructureBuilder.DOLLAR_SIGN + name, strictMatch);
            if (openField instanceof DecisionRowField field) {
                return new ExprConditionOrActionField(field);
            }
        }
        return null;
    }
}
