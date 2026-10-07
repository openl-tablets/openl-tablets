package org.openl.studio.projects.model.tests;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.testmethod.TestStatus;

/**
 * Result of a test assertion execution.
 */
@Builder
public record TestAssertionExecutionResult(
        @Parameter(description = "Description of the assertion")
        String description,

        @Schema(description = "Expected value of the assertion", implementation = Object.class)
        JsonNode expectedValue,

        @Schema(description = "Actual value of the assertion", implementation = Object.class)
        JsonNode actualValue,

        @Parameter(description = """
                Present and true when the actual value is referred to instead of written: it has inner structure, \
                and the summary was asked for lazy values or the server gave the value back to free memory. It is \
                read a level at a time; a value given back runs its case again""")
        @Nullable Boolean actualLazy,

        @Parameter(description = """
                Present and true when the expected value is referred to instead of written: it has inner structure, \
                and the summary was asked for lazy values. It is read a level at a time""")
        @Nullable Boolean expectedLazy,

        @Parameter(description = "Status of the assertion execution")
        TestStatus status
) {
}
