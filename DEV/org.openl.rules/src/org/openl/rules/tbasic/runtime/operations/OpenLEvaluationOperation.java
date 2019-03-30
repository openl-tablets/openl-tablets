/**
 *
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.types.IMethodCaller;

/**
 * The <code>OpenLEvaluationOperation</code> class describes operation which
 * has expression which must be calculated.
 *
 * @author User
 *
 */
public abstract class OpenLEvaluationOperation extends RuntimeOperation {
    private IMethodCaller openLStatement;

    /**
     * Create an instance of <code>OpenLEvaluationOperation</code>.
     *
     * @param openLStatement Expression which result must be calculated.
     *
     */
    public OpenLEvaluationOperation(IMethodCaller openLStatement) {
        this.openLStatement = openLStatement;
    }

    /**
     * Calculate expression in specified context.
     *
     * @param environment Environment for execution.
     * @return The result of expression
     */
    public Object evaluateStatement(TBasicContextHolderEnv environment) {
        Object resultValue = null;

        if (openLStatement != null) {
            resultValue = openLStatement.invoke(environment.getTbasicTarget(), environment.getTbasicParams(),
                    environment);
        }

        return resultValue;
    }

    public IMethodCaller getOpenLStatement() {
        return openLStatement;
    }

}
