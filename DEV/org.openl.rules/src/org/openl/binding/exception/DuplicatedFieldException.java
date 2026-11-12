package org.openl.binding.exception;

import java.io.Serial;

import lombok.Getter;

import org.openl.exception.OpenlNotCheckedException;

public class DuplicatedFieldException extends OpenlNotCheckedException {

    @Serial
    private static final long serialVersionUID = 2754037692502108330L;

    @Getter
    private final String fieldName;

    private final String kind;

    public DuplicatedFieldException(String fieldName) {
        this("Field", fieldName);
    }

    /**
     * @param kind      what the name belongs to, such as {@code Field} or {@code Test table}
     * @param fieldName the name defined twice
     */
    public DuplicatedFieldException(String kind, String fieldName) {
        this.kind = kind;
        this.fieldName = fieldName;
    }

    @Override
    public String getMessage() {
        return "%s '%s' has already been defined.".formatted(kind, fieldName);
    }

}
