package org.openl.binding.exception;

import java.io.Serial;

import org.openl.exception.OpenlNotCheckedException;

public class DuplicatedTypeException extends OpenlNotCheckedException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String type;

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

        sb.append("Type '%s' has already been defined.".formatted(type));
        return sb.toString();
    }

}
