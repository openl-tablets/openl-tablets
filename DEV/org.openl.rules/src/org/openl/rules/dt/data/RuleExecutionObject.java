package org.openl.rules.dt.data;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DelegatedDynamicObject;

/**
 * The object is the reference to the current rule that is being executed at the moment. Currently it is valid only
 * inside of the action that is being executed. The object facilitates the access from the action method to the
 * variables defined in conditions of the same rule
 *
 * In the future we may want to provide similar functionality inside conditions as well, but it may not be easy because
 * of the indexing and optimization.
 *
 * @author snshor Created Jun 15, 2010
 */
public class RuleExecutionObject extends DelegatedDynamicObject {

    private int ruleNum; // the index of the current rule that is being executed

    public RuleExecutionObject(IOpenClass rulesType, IDynamicObject target, int ruleNum) {
        super(rulesType, target);
        this.ruleNum = ruleNum;
    }

    int getRuleNum() {
        return ruleNum;
    }
}
