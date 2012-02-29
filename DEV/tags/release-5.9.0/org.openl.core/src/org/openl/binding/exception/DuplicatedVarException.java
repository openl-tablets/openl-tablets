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
    
    private String fieldName;

    public DuplicatedVarException(String msg, String fieldName) {
        super(msg);        
        this.fieldName = fieldName;
    }

    @Override
    public String getMessage() {
        StringBuffer buf = new StringBuffer();
        if (super.getMessage() != null) {
            buf.append(super.getMessage());
        }

        buf.append(String.format("Variable %s has already been defined", fieldName));
        return buf.toString();
    }

}
