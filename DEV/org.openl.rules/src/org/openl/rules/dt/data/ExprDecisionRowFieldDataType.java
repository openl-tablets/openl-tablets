package org.openl.rules.dt.data;

import org.openl.OpenL;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.types.IOpenField;

class ExprDecisionRowFieldDataType extends ComponentOpenClass {

    private final ConditionOrActionDataType conditionOrActionDataType;

    ExprDecisionRowFieldDataType(ConditionOrActionDataType conditionOrActionDataType, OpenL openl) {
        super(ExprDecisionRowFieldDataType.class.getSimpleName(), openl);
        this.conditionOrActionDataType = conditionOrActionDataType;
    }

    @Override
    public IOpenField getField(String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField openField = conditionOrActionDataType.getField(name, strictMatch);
        if (openField instanceof ConditionOrActionParameterField) {
            return new ExprParameterField((ConditionOrActionParameterField) openField);
        }
        return null;
    }
}
