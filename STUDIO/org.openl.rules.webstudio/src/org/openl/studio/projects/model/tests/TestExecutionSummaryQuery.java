package org.openl.studio.projects.model.tests;

import io.swagger.v3.oas.annotations.media.Schema;

public record TestExecutionSummaryQuery(
        @Schema(description = "If true, only failed test units are included in the summary. Default is false.")
        boolean failedOnly,

        @Schema(description = """
                The maximum number of failed test units to include in the summary. A count of -1 keeps every \
                one of them.""")
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
    private static final TestExecutionSummaryQuery LAZY = new TestExecutionSummaryQuery(false, 5, false, true);

    private static final TestExecutionSummaryQuery IN_FULL = new TestExecutionSummaryQuery(false, 5, true, false);

    /**
     * Every case, the way the screen reads the results: a value with inner structure is referred to instead of
     * written, and no schema is written. It is what a test table is announced with as soon as it has run.
     */
    public static TestExecutionSummaryQuery lazy() {
        return LAZY;
    }

    /** Every value written out, the whole returned value included. It is what a single case is read with. */
    public static TestExecutionSummaryQuery inFull() {
        return IN_FULL;
    }
}
