package org.openl.studio.projects.model.tests;

import io.swagger.v3.oas.annotations.media.Schema;

public record TestExecutionSummaryQuery(
        @Schema(description = "If true, only failed test units are included in the summary. Default is false.")
        boolean failedOnly,

        @Schema(description = "The maximum number of failed test units to include in the summary. Default is 5.")
        int failures
) {
    public TestExecutionSummaryQuery {
        failures = failures == 0 ? 5 : failures;
    }

    private static final TestExecutionSummaryQuery NO_FILTER = new TestExecutionSummaryQuery(false, 5);

    public static TestExecutionSummaryQuery noFilter() {
        return NO_FILTER;
    }
}
