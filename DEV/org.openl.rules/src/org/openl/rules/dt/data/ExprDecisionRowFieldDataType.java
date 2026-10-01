package org.openl.rules.dt.data;

import org.openl.OpenL;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.types.IOpenField;

// OpenL types are equal by their instance class, which binding and casts rely on; the state added here is not identity.
@SuppressWarnings("java:S2160")
class ExprDecisionRowFieldDataType extends ComponentOpenClass {

    private final ConditionOrActionDataType conditionOrActionDataType;

    ExprDecisionRowFieldDataType(ConditionOrActionDataType conditionOrActionDataType, OpenL openl) {
        super(ExprDecisionRowFieldDataType.class.getSimpleName(), openl);
        this.conditionOrActionDataType = conditionOrActionDataType;
    }

    @Override
    public IOpenField getField(String name, boolean strictMatch) throws AmbiguousFieldException {
        var openField = conditionOrActionDataType.getField(name, strictMatch);
        if (openField instanceof ConditionOrActionParameterField field) {
            return new ExprParameterField(field);
        }
        return null;
    }
}
