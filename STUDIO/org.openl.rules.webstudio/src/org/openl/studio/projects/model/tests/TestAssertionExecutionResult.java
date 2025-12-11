package org.openl.studio.projects.model.tests;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.testmethod.TestStatus;

/**
 * Result of a test assertion execution.
 */
public record TestAssertionExecutionResult(
        @Parameter(description = "Description of the assertion")
        String description,

        @Schema(description = "Expected value of the assertion", implementation = Object.class)
        JsonNode expectedValue,

        @Schema(description = "Actual value of the assertion", implementation = Object.class)
        JsonNode actualValue,

        @Parameter(description = "Status of the assertion execution")
        TestStatus status
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String description;
        private JsonNode expectedValue;
        private JsonNode actualValue;
        private TestStatus status;

        private Builder() {
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder expectedValue(JsonNode expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        public Builder actualValue(JsonNode actualValue) {
            this.actualValue = actualValue;
            return this;
        }

        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }

        public TestAssertionExecutionResult build() {
            return new TestAssertionExecutionResult(description, expectedValue, actualValue, status);
        }
    }

}
