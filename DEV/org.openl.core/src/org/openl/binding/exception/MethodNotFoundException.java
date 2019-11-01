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

    private final String methodName;
    private final IOpenClass[] params;

    public MethodNotFoundException(String methodName, IOpenClass... params) {
        this.methodName = methodName;
        this.params = params;
    }

    @Override
    public String getMessage() {

        StringBuilder buffer = new StringBuilder();

        if (super.getMessage() != null) {
            buffer.append(super.getMessage());
        }

        buffer.append("Method '");
        MethodUtil.printMethod(methodName, params, buffer);
        buffer.append("' is not found.");

        return buffer.toString();
    }
}
