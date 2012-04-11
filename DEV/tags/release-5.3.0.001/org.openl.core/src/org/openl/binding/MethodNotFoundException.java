/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class MethodNotFoundException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = -6505424809898412642L;
    String msg;
    String methodName;
    IOpenClass[] pars;

    public MethodNotFoundException(String msg, String methodName, IOpenClass[] pars) {
        this.msg = msg;
        this.methodName = methodName;
        this.pars = pars;
    }

    @Override
    public String getMessage() {
        StringBuffer buf = new StringBuffer();
        if (msg != null) {
            buf.append(msg);
        }

        buf.append("Method ");
        MethodUtil.printMethod(methodName, pars, buf);
        buf.append(" is not found");
        return buf.toString();
    }

}
