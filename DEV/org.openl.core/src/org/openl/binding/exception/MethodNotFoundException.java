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

        StringBuilder sb = new StringBuilder();

        if (super.getMessage() != null) {
            sb.append(super.getMessage());
        }

        sb.append("Method '");
        MethodUtil.printMethod(methodName, params, sb);
        sb.append("' is not found.");

        return sb.toString();
    }
}
