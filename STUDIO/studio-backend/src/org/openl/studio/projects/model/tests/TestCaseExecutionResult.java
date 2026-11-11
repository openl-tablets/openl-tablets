package org.openl.studio.projects.model.tests;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import lombok.Singular;
import org.jspecify.annotations.Nullable;

@Builder
public record TestCaseExecutionResult(
        @Parameter(description = "Name of the test case")
        String name,

        @Parameter(description = "Test table identifier")
        String tableId,

        @Parameter(description = "Module the test table is written in, so a reader can be sent to it. It is a module of the tested project or of a project it depends on. Absent when no such module holds the table.")
        @Nullable String module,

        @Parameter(description = "Identifier of the project the test table is written in, as the Projects API addresses it, when it is a project the tested one depends on. Absent for a test table of the tested project.")
        @Nullable String projectId,

        @Parameter(description = "Description of the test case")
        String description,

        @Parameter(description = "Execution time of the test case in milliseconds")
        double executionTimeMs,

        @Parameter(description = """
                Total number of tests in the test case. Every test of a test table whose tested rules do not \
                compile is counted, and counted as failed""")
        int numberOfTests,

        @Parameter(description = "Number of failed tests in the test case")
        int numberOfFailures,

        @Parameter(description = "`true` when the table is a Run table, which states no expected values and so has nothing to pass or fail; absent for a test table")
        @Nullable Boolean runTable,

        @Parameter(description = "List of test unit execution results")
        @Singular("testUnit")
        List<TestUnitExecutionResult> testUnits
) {
}
