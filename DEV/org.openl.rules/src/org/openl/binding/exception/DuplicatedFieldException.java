package org.openl.binding.exception;

import java.io.Serial;

import org.openl.exception.OpenlNotCheckedException;

public class DuplicatedFieldException extends OpenlNotCheckedException {

    @Serial
    private static final long serialVersionUID = 2754037692502108330L;

    private final String fieldName;

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

        sb.append("Field '%s' has already been defined.".formatted(fieldName));
        return sb.toString();
    }

}
