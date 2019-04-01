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

    private IOpenMethod existedMethod;
    private IOpenMethod newMethod;

    public DuplicatedMethodException(String msg, IOpenMethod existedMethod, IOpenMethod newMethod) {
        super(msg);
        this.existedMethod = existedMethod;
        this.newMethod = newMethod;
    }

    public IOpenMethod getExistedMethod() {
        return existedMethod;
    }

    public IOpenMethod getNewMethod() {
        return newMethod;
    }

    @Override
    public String getMessage() {
        if (super.getMessage() != null) {
            return super.getMessage();
        }

        StringBuilder buf = new StringBuilder();
        buf.append("Method ");
        MethodUtil.printMethod(newMethod, buf);
        buf.append(" has already been defined.");
        return buf.toString();
    }

}
