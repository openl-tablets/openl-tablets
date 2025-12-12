package org.openl.studio.projects.model.tests;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

public record TestCaseExecutionResult(
        @Parameter(description = "Name of the test case")
        String name,

        @Parameter(description = "Test table identifier")
        String tableId,

        @Parameter(description = "Description of the test case")
        String description,

        @Parameter(description = "Execution time of the test case in milliseconds")
        double executionTimeMs,

        @Parameter(description = "Total number of tests in the test case")
        int numberOfTests,

        @Parameter(description = "Number of failed tests in the test case")
        int numberOfFailures,

        @Parameter(description = "List of test unit execution results")
        List<TestUnitExecutionResult> testUnits
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;
        private String tableId;
        private String description;
        private double executionTimeMs;
        private int numberOfTests;
        private int numberOfFailures;
        private final List<TestUnitExecutionResult> testUnits = new ArrayList<>();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
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

        public Builder numberOfTests(int numberOfTests) {
            this.numberOfTests = numberOfTests;
            return this;
        }

        public Builder numberOfFailures(int numberOfFailures) {
            this.numberOfFailures = numberOfFailures;
            return this;
        }

        public Builder tableId(String tableId) {
            this.tableId = tableId;
            return this;
        }

        public Builder putTestUnit(TestUnitExecutionResult testUnit) {
            this.testUnits.add(testUnit);
            return this;
        }

        public TestCaseExecutionResult build() {
            return new TestCaseExecutionResult(name,
                    tableId,
                    description,
                    executionTimeMs,
                    numberOfTests,
                    numberOfFailures,
                    List.copyOf(testUnits));
        }
    }

}
