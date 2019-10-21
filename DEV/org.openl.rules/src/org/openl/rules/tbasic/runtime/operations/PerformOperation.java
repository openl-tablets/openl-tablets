package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.types.IMethodCaller;

/**
 * The <code>PerformOperation</code> class describes simple class which calculates some statement, but does not requires
 * any return.
 *
 * @author User
 *
 */
public class PerformOperation extends OpenLEvaluationOperation {

    /**
     * Create an instance of <code>PerformOperation</code>.
     *
     * @param openLStatement Statement which result must be calculated.
     */
    public PerformOperation(IMethodCaller openLStatement) {
        super(openLStatement);
    }

    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        Object returnValue = evaluateStatement(environment);
        return new Result(ReturnType.NEXT, returnValue);
    }

}
