package org.openl.rules.dt.data;

import org.openl.OpenL;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dt.IBaseDecisionRow;

class ConditionOrActionDataType extends ComponentOpenClass {

    ConditionOrActionDataType(IBaseDecisionRow conditionOrAction, OpenL openl) {
        super(conditionOrAction.getName(), openl);
        var pdd = conditionOrAction.getParams();
        for (var i = 0; i < pdd.length; i++) {
            if (pdd[i] != null) {
                addField(new ConditionOrActionParameterField(conditionOrAction, i));
            }
        }
    }

}
