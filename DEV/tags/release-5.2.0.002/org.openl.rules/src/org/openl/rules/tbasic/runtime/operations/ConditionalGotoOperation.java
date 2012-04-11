/**
 * 
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * @author User
 * 
 */
public class ConditionalGotoOperation extends GotoOperation {

    private boolean expectedCondition;

    public ConditionalGotoOperation(String label, boolean expectedCondition) {
        super(label);
        this.expectedCondition = expectedCondition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules
     * .tbasic.runtime.TBasicContext, java.lang.Object[])
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        assert param != null;
        assert param instanceof Boolean;

        boolean condition = (Boolean) param;
        Result result;

        if (condition == expectedCondition) {
            result = executeGoto(environment);
        } else {
            result = skipGoto();
        }

        return result;
    }

    /**
     * @return
     */
    private Result skipGoto() {
        Result result;
        result = new Result(ReturnType.Next);
        return result;
    }

    /**
     * @param environment
     * @return
     */
    private Result executeGoto(TBasicContextHolderEnv environment) {
        Result result;
        result = super.execute(environment, null);
        return result;
    }
}
