package org.openl.rules.rest.common.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Base Error model for handling Exceptions in WebStudio REST API
 * 
 * @author Vladyslav Pikus
 * @see org.openl.rules.rest.common.ApiExceptionControllerAdvice
 */
public class BaseError {

    @Schema(description = "Error code")
    public final String code;

    @Schema(description = "Localized error message")
    public final String message;

    protected BaseError(Builder from) {
        this.code = from.code;
        this.message = from.message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String code;
        private String message;

        protected Builder() {
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public BaseError build() {
            return new BaseError(this);
        }
    }

}
