package org.openl.rules.dt.data;

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.dt.DecisionTableRuntimePool;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DelegatedDynamicObject;

/**
 * The object is the reference to the current rule that is being executed at the moment. Currently it is valid only
 * inside of the action that is being executed. The object facilitates the access from the action method to the
 * variables defined in conditions of the same rule
 * <p>
 * In the future we may want to provide similar functionality inside conditions as well, but it may not be easy because
 * of the indexing and optimization.
 *
 * @author snshor Created Jun 15, 2010
 */
public class RuleExecutionObject extends DelegatedDynamicObject {

    @Getter(AccessLevel.PACKAGE)
    private final int ruleNum; // the index of the current rule that is being executed

    /** What the call of the table has computed already, or {@code null} outside a call. */
    @Getter
    private final @Nullable DecisionTableRuntimePool pool;

    public RuleExecutionObject(IOpenClass rulesType,
                               IDynamicObject target,
                               int ruleNum,
                               @Nullable DecisionTableRuntimePool pool) {
        super(rulesType, target);
        this.ruleNum = ruleNum;
        this.pool = pool;
    }
}
