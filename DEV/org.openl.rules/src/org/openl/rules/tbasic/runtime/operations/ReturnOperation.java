/**
 *
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.types.IMethodCaller;

/**
 * The <code>ReturnOperation</code> class describes operation which finalizes execution of some function or subroutine
 * and returns some value if necessary.
 *
 * @author User
 *
 */
public class ReturnOperation<ResultValueType> extends OpenLEvaluationOperation {
    private boolean hasReturnValue;

    /**
     * Create an instance of <code>ReturnOperation</code>.
     *
     * @param openLStatement Expression which result must be calculated and returned. If openLStatement is blank then
     *            operation just end execution of some function.
     */
    public ReturnOperation(IMethodCaller openLStatement) {
        super(openLStatement);
        hasReturnValue = openLStatement != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext,
     * java.lang.Object[])
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        Object resultValue = null;

        if (hasReturnValue) {
            resultValue = evaluateStatement(environment);
        }

        return new Result(ReturnType.RETURN, resultValue);
    }

}
