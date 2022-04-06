package org.openl.rules.rest.common.model;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Field Validation Error model for handling Binding Exceptions (validation) in WebStudio REST API
 *
 * @author Vladyslav Pikus
 * @see org.openl.rules.rest.common.ApiExceptionControllerAdvice
 */
public final class FieldError extends BaseError {

    @Parameter(description = "Affected field of the validated object")
    private final String field;

    @Parameter(description = "Rejected field value")
    private final Object rejectedValue;

    private FieldError(Builder from) {
        super(from);
        this.field = from.field;
        this.rejectedValue = from.rejectedValue;
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseError.Builder {

        private String field;
        private Object rejectedValue;

        private Builder() {
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder rejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
            return this;
        }

        @Override
        public Builder code(String code) {
            super.code(code);
            return this;
        }

        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public FieldError build() {
            return new FieldError(this);
        }
    }
}
