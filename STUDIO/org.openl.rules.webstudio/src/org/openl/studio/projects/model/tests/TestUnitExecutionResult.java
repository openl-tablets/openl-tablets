package org.openl.studio.projects.model.tests;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.TestStatus;

public class TestUnitExecutionResult {

    @Parameter(description = "Identifier of the test unit")
    private final String id;

    @Parameter(description = "Description of the test unit")
    private final String description;

    @Parameter(description = "Execution time of the test unit in milliseconds")
    private final double executionTimeMs;

    @Parameter(description = "Status of the test unit execution")
    private final TestStatus status;

    @Parameter(description = "List of test assertion execution results")
    private final List<TestAssertionExecutionResult> testAssertions;

    @Parameter(description = "List of test parameter values")
    private final List<TestParameterValue> parameters;

    @Parameter(description = "List of context parameter values")
    private final List<TestParameterValue> contextParameters;

    @Parameter(description = "List of error messages occurred during test unit execution")
    private final List<MessageDescription> errors;

    public TestUnitExecutionResult(Builder builder) {
        this.id = builder.id;
        this.description = builder.description;
        this.executionTimeMs = builder.executionTimeMs;
        this.status = builder.status;
        this.testAssertions = List.copyOf(builder.testAssertions);
        this.parameters = List.copyOf(builder.parameters);
        this.contextParameters = List.copyOf(builder.contextParameters);
        this.errors = List.copyOf(builder.errors);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public TestStatus getStatus() {
        return status;
    }

    public List<TestAssertionExecutionResult> getTestAssertions() {
        return testAssertions;
    }

    public List<TestParameterValue> getParameters() {
        return parameters;
    }

    public List<TestParameterValue> getContextParameters() {
        return contextParameters;
    }

    public List<MessageDescription> getErrors() {
        return errors;
    }

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
            return new TestUnitExecutionResult(this);
        }

    }

}
