/**
 *
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.types.IMethodCaller;

/**
 * The <code>CalculateOperation</code> class describes simple class which calculates some expression.
 *
 * @author User
 *
 */
public class CalculateOperation extends OpenLEvaluationOperation {

    /**
     * Create an instance of <code>CalculateOperation</code>.
     *
     * @param openLStatement Expression which result must be calculated.
     */
    public CalculateOperation(IMethodCaller openLStatement) {
        super(openLStatement);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext,
     * java.lang.Object[])
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        Object resultValue = evaluateStatement(environment);
        return new Result(ReturnType.NEXT, resultValue);
    }

}
