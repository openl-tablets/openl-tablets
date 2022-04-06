package org.openl.rules.rest.common.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Validation Error model for handling Binding Exceptions (validation) in WebStudio REST API
 *
 * @author Vladyslav Pikus
 * @see org.openl.rules.rest.common.ApiExceptionControllerAdvice
 */
public final class ValidationError extends BaseError {

    @Parameter(description = "Field errors")
    private final List<FieldError> fields;

    @Parameter(description = "Additional Global Errors")
    private final List<BaseError> errors;

    private ValidationError(Builder from) {
        super(from);
        this.fields = new ArrayList<>(from.fields);
        this.errors = new ArrayList<>(from.errors);
    }

    public List<FieldError> getFields() {
        return fields;
    }

    public List<BaseError> getErrors() {
        return errors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseError.Builder {

        private final List<FieldError> fields = new ArrayList<>();
        private final List<BaseError> errors = new ArrayList<>();

        private Builder() {
        }

        public Builder addField(FieldError field) {
            fields.add(field);
            return this;
        }

        public Builder addError(BaseError error) {
            errors.add(error);
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
        public ValidationError build() {
            return new ValidationError(this);
        }
    }

}
