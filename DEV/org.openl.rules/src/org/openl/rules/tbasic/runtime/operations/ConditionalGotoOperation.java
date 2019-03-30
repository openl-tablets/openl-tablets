/**
 *
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * The <code>CalculateOperation</code> class describes GOTO operation which will be realized if previous condition
 * returned expected result.
 *
 * @author User
 *
 */
public class ConditionalGotoOperation extends GotoOperation {

    private boolean expectedCondition;

    /**
     * Create an instance of <code>ConditionalGotoOperation</code>.
     *
     * @param label The label to jump to.
     * @param expectedCondition expected result of previous calculation of condition(if equals then GOTO will executed
     *            else jump to next operation will be performed).
     */
    public ConditionalGotoOperation(String label, boolean expectedCondition) {
        super(label);
        this.expectedCondition = expectedCondition;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules .tbasic.runtime.TBasicContext,
     * java.lang.Object[])
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
     * @param environment
     * @return
     */
    private Result executeGoto(TBasicContextHolderEnv environment) {
        Result result;
        result = super.execute(environment, null);
        return result;
    }

    /**
     * @return
     */
    private Result skipGoto() {
        Result result;
        result = new Result(ReturnType.NEXT);
        return result;
    }
}
