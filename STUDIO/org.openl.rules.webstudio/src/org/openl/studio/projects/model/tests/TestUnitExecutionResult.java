package org.openl.studio.projects.model.tests;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.TestStatus;

public record TestUnitExecutionResult(
        @Parameter(description = "Identifier of the test unit")
        String id,

        @Parameter(description = "Description of the test unit")
        String description,

        @Parameter(description = "Execution time of the test unit in milliseconds")
        double executionTimeMs,

        @Parameter(description = "Status of the test unit execution")
        TestStatus status,

        @Parameter(description = "List of test assertion execution results")
        List<TestAssertionExecutionResult> testAssertions,

        @Parameter(description = "List of test parameter values")
        List<TestParameterValue> parameters,

        @Parameter(description = "List of context parameter values")
        List<TestParameterValue> contextParameters,

        @Parameter(description = "List of error messages occurred during test unit execution")
        List<MessageDescription> errors
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private String description;
        private double executionTimeMs;
        private TestStatus status;
        private final List<TestAssertionExecutionResult> testAssertions = new ArrayList<>();
        private final List<TestParameterValue> parameters = new ArrayList<>();
        private final List<TestParameterValue> contextParameters = new ArrayList<>();
        private final List<MessageDescription> errors = new ArrayList<>();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder executionTimeMs(double executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }

        public Builder putTestAssertion(TestAssertionExecutionResult testAssertion) {
            this.testAssertions.add(testAssertion);
            return this;
        }

        public Builder putParameter(TestParameterValue parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public Builder putContextParameter(TestParameterValue contextParameter) {
            this.contextParameters.add(contextParameter);
            return this;
        }

        public Builder putErrorMessage(MessageDescription error) {
            this.errors.add(error);
            return this;
        }

        public TestUnitExecutionResult build() {
            return new TestUnitExecutionResult(id,
                    description,
                    executionTimeMs,
                    status,
                    List.copyOf(testAssertions),
                    List.copyOf(parameters),
                    List.copyOf(contextParameters),
                    List.copyOf(errors));
        }

    }

}
