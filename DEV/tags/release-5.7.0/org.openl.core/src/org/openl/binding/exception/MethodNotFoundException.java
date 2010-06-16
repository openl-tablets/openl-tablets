/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import org.openl.binding.MethodUtil;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class MethodNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -6505424809898412642L;

    private String message;
    private String methodName;
    private IOpenClass[] params;

    public MethodNotFoundException(String message, String methodName, IOpenClass[] params) {

        this.message = message;
        this.methodName = methodName;
        this.params = params;
    }

    @Override
    public String getMessage() {

        StringBuffer buffer = new StringBuffer();

        if (message != null) {
            buffer.append(message);
        }

        buffer.append("Method ");
        MethodUtil.printMethod(methodName, params, buffer);
        buffer.append(" is not found");

        return buffer.toString();
    }
}
