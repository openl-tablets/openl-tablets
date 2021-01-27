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
    private final IOpenClass target;

    public MethodNotFoundException(String methodName, IOpenClass... params) {
        this.methodName = methodName;
        this.params = params;
        this.target = null;
    }

    public MethodNotFoundException(IOpenClass target, String methodName,  IOpenClass... params) {
        this.methodName = methodName;
        this.params = params;
        this.target = target;
    }

    @Override
    public String getMessage() {

        StringBuilder sb = new StringBuilder();

        if (super.getMessage() != null) {
            sb.append(super.getMessage());
        }

        sb.append(target != null && target.isStatic() ? "Static method '" : "Method '");
        MethodUtil.printMethod(methodName, params, sb);
        sb.append("' is not found");

        if (target != null) {
            sb.append(" in type '").append(target.getName()).append("'");
        }

        sb.append(".");

        return sb.toString();
    }
}
