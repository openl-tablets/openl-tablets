/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

/**
 * @author snshor
 *
 */
public class DuplicatedVarException extends RuntimeException {
    private static final long serialVersionUID = 2754037692502108330L;
    private String msg;
    private String fieldName;

    public DuplicatedVarException(String msg, String fieldName) {
        this.msg = msg;
        this.fieldName = fieldName;
    }

    @Override
    public String getMessage() {
        StringBuffer buf = new StringBuffer();
        if (msg != null) {
            buf.append(msg);
        }

        buf.append(String.format("Variable %s has already been defined", fieldName));
        return buf.toString();
    }

}
