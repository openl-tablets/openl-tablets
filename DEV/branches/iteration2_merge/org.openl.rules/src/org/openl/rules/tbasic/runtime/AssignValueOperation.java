/**
 * 
 */
package org.openl.rules.tbasic.runtime;

/**
 * @author User
 *
 */
public class AssignValueOperation extends RuntimeOperation {

    private String variableName;
    
    public AssignValueOperation(String variableName){
        this.variableName = variableName;
    }
    
    /* (non-Javadoc)
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext, java.lang.Object)
     */
    @Override
    public Result execute(TBasicContext context, Object param) {
        context.assignValueToVariable(variableName, param);
        
        // Return variable value to allow multiple concurrent assignments   
        return new Result(ReturnType.Next, param);
    }

}
