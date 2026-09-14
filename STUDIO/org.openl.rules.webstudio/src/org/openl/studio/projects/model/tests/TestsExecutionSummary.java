package org.openl.studio.projects.model.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

import org.openl.rules.repository.api.Pageable;
import org.openl.studio.common.model.PageResponse;

public class TestsExecutionSummary extends PageResponse<TestCaseExecutionResult> {

    @Getter
    @Parameter(description = "Total execution time of all tests in milliseconds")
    private final double executionTimeMs;

    @Getter
    @Parameter(description = "Total number of tests executed")
    private final int numberOfTests;

    @Getter
    @Parameter(description = "Total number of failed tests")
    private final int numberOfFailures;

    private TestsExecutionSummary(Builder builder) {
        super(builder.testCases,
                builder.page.isUnpaged()
                        ? -1
                        : builder.page.getPageNumber(),
                builder.page.isUnpaged()
                        ? builder.testCases.size()
                        : builder.page.getPageSize(),
                builder.total);
        this.executionTimeMs = builder.executionTimeMs;
        this.numberOfTests = builder.numberOfTests;
        this.numberOfFailures = builder.numberOfFailures;
    }

    @Override
    @Parameter(description = "List of test case execution results")
    @JsonProperty("testCases")
    public Collection<TestCaseExecutionResult> getContent() {
        return super.getContent();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private double executionTimeMs;
        private int numberOfTests;
        private int numberOfFailures;
        private long total;
        private Pageable page;
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

        public Builder page(Pageable page) {
            this.page = page;
            return this;
        }

        /** How many test tables ran in all, of which the page below carries some. */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public TestsExecutionSummary build() {
            return new TestsExecutionSummary(this);
        }
    }

}
