/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class MethodNotFoundException extends OpenlNotCheckedException {

    private static final long serialVersionUID = -6505424809898412642L;
    
    private String methodName;
    private IOpenClass[] params;

    public MethodNotFoundException(String message, String methodName, IOpenClass[] params) {
        super(message);
        this.methodName = methodName;
        this.params = params;
    }

    @Override
    public String getMessage() {

        StringBuffer buffer = new StringBuffer();

        if (super.getMessage() != null) {
            buffer.append(super.getMessage());
        }

        buffer.append("Method ");
        MethodUtil.printMethod(methodName, params, buffer);
        buffer.append(" is not found");

        return buffer.toString();
    }
}
