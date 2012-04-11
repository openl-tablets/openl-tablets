/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public class DuplicatedMethodException extends OpenlNotCheckedException {
    /**
     *
     */
    private static final long serialVersionUID = 4145939391957085009L;
    
    private IOpenMethod method;

    public DuplicatedMethodException(String msg, IOpenMethod method) {
        super(msg);
        this.method = method;
    }

    @Override
    public String getMessage() {
        if (super.getMessage() != null) {
            return super.getMessage();
        }

        StringBuffer buf = new StringBuffer();
        buf.append("Method ");
        MethodUtil.printMethod(method, buf);
        buf.append(" has already been defined");
        return buf.toString();
    }

}
