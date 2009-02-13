/**
 * 
 */
package org.openl.rules.tbasic.runtime;

/**
 * @author User
 *
 */
public class GotoOperation extends RuntimeOperation {

    protected String label;
    
    public GotoOperation(String label){
        this.label = label;
    }
    
    /* (non-Javadoc)
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext, java.lang.Object[])
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        return new Result(ReturnType.Goto, label);
    }

}
