package org.openl.rules.dt;

import org.openl.rules.dt.IBaseAction;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Created by ymolchan on 05.02.2016.
 */
public class ActionInvoker implements Invokable {

    private final int ruleN;
    private final IBaseAction[] actions;

    ActionInvoker(int ruleN, IBaseAction[] actions) {
        this.ruleN = ruleN;
        this.actions = actions;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Object returnValue = null;
        for (IBaseAction action : actions) {

            Object actionResult = action.executeAction(ruleN, target, params, env);

            if (action
                    .isReturnAction() && returnValue == null && (actionResult != null || (!action.isEmpty(ruleN)))) {
                returnValue = actionResult;
            }
        }
        return returnValue;
    }

    public int getRule() {
        return ruleN;
    }
}
