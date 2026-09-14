package org.openl.studio.projects.model.tests;

import io.swagger.v3.oas.annotations.media.Schema;

public record TestExecutionSummaryQuery(
        @Schema(description = "If true, only failed test units are included in the summary. Default is false.")
        boolean failedOnly,

        @Schema(description = "The maximum number of failed test units to include in the summary. Default is 5.")
        int failures,

        @Schema(description = """
                If true, every test unit also carries the whole value the tested rule returned. Default is \
                false.""")
        boolean compoundResult,

        @Schema(description = """
                If true, a test input with inner structure is referred to instead of written, and no schema is \
                written at all. Default is false.""")
        boolean lazyValues
) {
    public TestExecutionSummaryQuery {
        failures = failures == 0 ? 5 : failures;
    }

    private static final TestExecutionSummaryQuery NO_FILTER = new TestExecutionSummaryQuery(false, 5, false, false);

    private static final TestExecutionSummaryQuery IN_FULL = new TestExecutionSummaryQuery(false, 5, true, false);

    public static TestExecutionSummaryQuery noFilter() {
        return NO_FILTER;
    }

    /** Every value written out, the whole returned value included. It is what a single case is read with. */
    public static TestExecutionSummaryQuery inFull() {
        return IN_FULL;
    }
}
