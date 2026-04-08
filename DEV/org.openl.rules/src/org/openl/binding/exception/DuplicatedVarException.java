/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import java.io.Serial;

import org.openl.exception.OpenlNotCheckedException;

/**
 * @author snshor
 */
public class DuplicatedVarException extends OpenlNotCheckedException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String variableName;

    public DuplicatedVarException(String msg, String variableName) {
        super(msg);
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (super.getMessage() != null) {
            sb.append(super.getMessage());
        }

        sb.append("Variable '%s' is already defined.".formatted(variableName));
        return sb.toString();
    }

}
