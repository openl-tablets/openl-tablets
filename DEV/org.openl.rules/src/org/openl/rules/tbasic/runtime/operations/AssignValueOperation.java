/**
 *
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * The <code>AssignValueOperation</code> class describes operation which creates new variable.
 *
 * @author User
 *
 */
public class AssignValueOperation extends RuntimeOperation {

    private String variableName;

    /**
     * Create an instance of <code>AssignValueOperation</code>.
     *
     * @param variableName Name of the new variable.
     */
    public AssignValueOperation(String variableName) {
        this.variableName = variableName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext,
     * java.lang.Object)
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        environment.assignValueToVariable(variableName, param);

        // Return variable value to allow multiple concurrent assignments
        return new Result(ReturnType.NEXT, param);
    }

}
