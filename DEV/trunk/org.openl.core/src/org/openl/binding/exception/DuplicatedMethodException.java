/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import org.openl.binding.MethodUtil;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public class DuplicatedMethodException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 4145939391957085009L;
    String msg;
    IOpenMethod method;

    public DuplicatedMethodException(String msg, IOpenMethod method) {
        this.msg = msg;
        this.method = method;
    }

    @Override
    public String getMessage() {
        if (msg != null) {
            return msg;
        }

        StringBuffer buf = new StringBuffer();
        buf.append("Method ");
        MethodUtil.printMethod(method, buf);
        buf.append(" has already been defined");
        return buf.toString();
    }

}
