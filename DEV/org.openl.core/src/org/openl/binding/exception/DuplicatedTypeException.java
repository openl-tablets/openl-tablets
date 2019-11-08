package org.openl.binding.exception;

import org.openl.exception.OpenlNotCheckedException;

public class DuplicatedTypeException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 1L;

    private String type;

    public DuplicatedTypeException(String msg, String type) {
        super(msg);
        this.type = type;
    }

    public String getFieldName() {
        return type;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (super.getMessage() != null) {
            sb.append(super.getMessage());
        }

        sb.append(String.format("Type '%s' has already been defined.", type));
        return sb.toString();
    }

}