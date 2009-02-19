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
public class NopOperation extends RuntimeOperation {

    /* (non-Javadoc)
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext, java.lang.Object)
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        // do nothing
        return new Result(ReturnType.Next);
    }

}
