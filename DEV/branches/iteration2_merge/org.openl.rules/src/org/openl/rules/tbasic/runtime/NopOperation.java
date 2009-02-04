/**
 * 
 */
package org.openl.rules.tbasic.runtime;

/**
 * @author User
 *
 */
public class NopOperation extends RuntimeOperation {

    /* (non-Javadoc)
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext, java.lang.Object)
     */
    @Override
    public Result execute(TBasicContext context, Object param) {
        // do nothing
        return new Result(ReturnType.Next);
    }

}
