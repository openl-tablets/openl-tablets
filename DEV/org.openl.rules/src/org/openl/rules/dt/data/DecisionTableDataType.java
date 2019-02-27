package org.openl.rules.dt.data;

import org.openl.OpenL;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.IDecisionRow;

/**
 * Provides access to the elements of the Decision table as data.
 * 
 * Each Condition and action becomes an internal type and parameters become attributes of this type.
 * 
 * Current implementation has the following limitations:
 * 
 * a) it supports only the access from action method to the variables defined in conditions. No access to other actions
 * is provided b) it will work only if variables in conditions are constants (not formulas) c) it does not provide
 * access to other rules than current one (for example we may want to access the previous rule via $previous.$C1.limit
 * or any random rule via $rules[7].$C1.limit etc.) d) the data is accessible only from inside of the DecisionTable,
 * there is no access from the outside, this will require a special meta-facility in the project to provide standardized
 * external access to internals of the different tables
 *
 * @author snshor Created Jun 15, 2010
 *
 */

public class DecisionTableDataType extends ComponentOpenClass {

    public DecisionTableDataType(DecisionTable dtable, String name, OpenL openl) {
        super(name, openl);
        for (IBaseCondition condOrAction : dtable.getConditionRows()) {
            ConditionOrActionDataType dataType = new ConditionOrActionDataType(condOrAction, this.getOpenl());
            addField(new DecisionRowField((IDecisionRow) condOrAction, dataType, this));
        }
    }

}
