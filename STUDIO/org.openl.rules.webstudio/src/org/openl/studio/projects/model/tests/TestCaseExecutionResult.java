package org.openl.studio.projects.model.tests;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import lombok.Singular;

@Builder
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
        @Singular("testUnit")
        List<TestUnitExecutionResult> testUnits
) {
}
