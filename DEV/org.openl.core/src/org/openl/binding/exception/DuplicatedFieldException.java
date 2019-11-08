package org.openl.binding.exception;

import org.openl.exception.OpenlNotCheckedException;

public class DuplicatedFieldException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 2754037692502108330L;

    private String fieldName;

    public DuplicatedFieldException(String msg, String fieldName) {
        super(msg);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (super.getMessage() != null) {
            sb.append(super.getMessage());
        }

        sb.append(String.format("Field '%s' has already been defined.", fieldName));
        return sb.toString();
    }

}
