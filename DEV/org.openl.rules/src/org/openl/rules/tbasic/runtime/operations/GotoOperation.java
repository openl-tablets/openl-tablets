/**
 *
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * The <code>CalculateOperation</code> class describes GOTO operation.
 *
 * @author User
 *
 */
public class GotoOperation extends RuntimeOperation {

    private String label;

    /**
     * Create an instance of <code>GotoOperation</code>.
     *
     * @param label The label to jump to.
     */
    public GotoOperation(String label) {
        this.label = label;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext,
     * java.lang.Object[])
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        return new Result(ReturnType.GOTO, label);
    }

}
