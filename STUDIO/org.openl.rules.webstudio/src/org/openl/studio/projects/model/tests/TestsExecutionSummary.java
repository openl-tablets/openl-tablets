package org.openl.studio.projects.model.tests;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

public class TestsExecutionSummary {

    @Parameter(description = "Total execution time of all tests in milliseconds")
    private final double executionTimeMs;

    @Parameter(description = "Total number of tests executed")
    private final int numberOfTests;

    @Parameter(description = "Total number of failed tests")
    private final int numberOfFailures;

    @Parameter(description = "List of test case execution results")
    private final List<TestCaseExecutionResult> testCases;

    public TestsExecutionSummary(Builder builder) {
        this.executionTimeMs = builder.executionTimeMs;
        this.numberOfTests = builder.numberOfTests;
        this.numberOfFailures = builder.numberOfFailures;
        this.testCases = List.copyOf(builder.testCases);
    }

    public List<TestCaseExecutionResult> getTestCases() {
        return testCases;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public int getNumberOfTests() {
        return numberOfTests;
    }

    public int getNumberOfFailures() {
        return numberOfFailures;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private double executionTimeMs;
        private int numberOfTests;
        private int numberOfFailures;
        private final List<TestCaseExecutionResult> testCases = new ArrayList<>();

        private Builder() {
        }

        public Builder putTestCase(TestCaseExecutionResult testCase) {
            this.testCases.add(testCase);
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

        public TestsExecutionSummary build() {
            return new TestsExecutionSummary(this);
        }
    }

}
