package org.openl.rules.dt.data;

import org.openl.OpenL;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.dt.DTColumnsDefinitionField;
import org.openl.types.IOpenField;

class DecisionExprFieldDataType extends ComponentOpenClass {
    private final DecisionTableDataType decisionTableDataType;

    DecisionExprFieldDataType(DecisionTableDataType decisionTableDataType, OpenL openl) {
        super(DecisionExprFieldDataType.class.getSimpleName(), openl);
        this.decisionTableDataType = decisionTableDataType;
    }

    @Override
    public IOpenField getField(String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField openField = decisionTableDataType.getField(name, strictMatch);
        if (openField instanceof DecisionRowField) {
            DecisionRowField decisionRowField = (DecisionRowField) openField;
            return new ExprDecisionRowField(decisionRowField, getOpenl());
        } else if (openField instanceof ConditionOrActionDirectParameterField) {
            return new ExprParameterField((ConditionOrActionDirectParameterField) openField);
        } else if (openField instanceof DTColumnsDefinitionField) {
            return new ExprParameterDTColumnsDefinitionField((DTColumnsDefinitionField) openField);
        } else if (openField == null && !name.startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN)) {
            openField = decisionTableDataType.getField(SpreadsheetStructureBuilder.DOLLAR_SIGN + name, strictMatch);
            if (openField instanceof DecisionRowField) {
                return new ExprConditionOrActionField((DecisionRowField) openField);
            }
        }
        return null;
    }
}
