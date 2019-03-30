/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import org.openl.exception.OpenLCompilationException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class FieldNotFoundException extends OpenLCompilationException {
    /**
     *
     */
    private static final long serialVersionUID = -782077307706500730L;

    private String fieldName;
    private IOpenClass type;

    public FieldNotFoundException(String msg, String fieldName, IOpenClass type) {
        super(msg);
        this.fieldName = fieldName;
        this.type = type;
    }

    @Override
    public String getMessage() {
        StringBuilder buf = new StringBuilder();
        if (super.getMessage() != null) {
            buf.append(super.getMessage());
        }

        buf.append("Field ").append(fieldName);
        buf.append(" is not found");
        if (type != null) {
            buf.append(" in ").append(type.getName());
        }
        return buf.toString();
    }

}
