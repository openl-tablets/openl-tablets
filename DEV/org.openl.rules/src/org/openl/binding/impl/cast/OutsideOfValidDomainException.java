package org.openl.binding.impl.cast;

import java.io.Serial;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.exception.OpenLRuntimeException;

@Getter
public class OutsideOfValidDomainException extends OpenLRuntimeException {

    @Serial
    private static final long serialVersionUID = 8536909104810936082L;

    /** The rejected value as text, or {@code null} when the exception does not name it. */
    private final @Nullable String value;
    private final @Nullable String vocabulary;
    private final @Nullable String allowedValues;

    public OutsideOfValidDomainException() {
        this.value = null;
        this.vocabulary = null;
        this.allowedValues = null;
    }

    public OutsideOfValidDomainException(String message, Throwable cause) {
        super(message, cause);
        this.value = null;
        this.vocabulary = null;
        this.allowedValues = null;
    }

    public OutsideOfValidDomainException(String message) {
        super(message);
        this.value = null;
        this.vocabulary = null;
        this.allowedValues = null;
    }

    public OutsideOfValidDomainException(Throwable cause) {
        super(cause);
        this.value = null;
        this.vocabulary = null;
        this.allowedValues = null;
    }

    public OutsideOfValidDomainException(Object value, String vocabulary, String allowedValues) {
        super("Object '%s' is outside of valid domain '%s'. Valid values: %s".formatted(
                value, vocabulary, allowedValues));
        this.value = String.valueOf(value);
        this.vocabulary = vocabulary;
        this.allowedValues = allowedValues;
    }

}
