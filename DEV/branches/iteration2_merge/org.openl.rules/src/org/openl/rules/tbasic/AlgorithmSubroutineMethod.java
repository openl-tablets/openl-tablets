/**
 * 
 */
package org.openl.rules.tbasic;

import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author User
 *
 */
public class AlgorithmSubroutineMethod extends AMethod implements IOpenMethod {

    public AlgorithmSubroutineMethod(IOpenMethodHeader header) {
        super(header);
    }

    /* (non-Javadoc)
     * @see org.openl.types.IMethodCaller#invoke(java.lang.Object, java.lang.Object[], org.openl.vm.IRuntimeEnv)
     */
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        // TODO Auto-generated method stub
        return null;
    }

}
