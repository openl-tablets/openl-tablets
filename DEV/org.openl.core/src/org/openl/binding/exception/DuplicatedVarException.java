/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import org.openl.exception.OpenlNotCheckedException;

/**
 * @author snshor
 *
 */
public class DuplicatedVarException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 2754037692502108330L;

    private String variableName;

    public DuplicatedVarException(String msg, String variableName) {
        super(msg);
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public String getMessage() {
        StringBuilder buf = new StringBuilder();
        if (super.getMessage() != null) {
            buf.append(super.getMessage());
        }

        buf.append(String.format("Variable '%s' has already been defined", variableName));
        return buf.toString();
    }

}
