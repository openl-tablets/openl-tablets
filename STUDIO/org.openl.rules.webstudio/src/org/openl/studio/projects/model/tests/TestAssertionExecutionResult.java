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
                Present and true when the actual value is referred to instead of written: it has inner structure \
                and the server gave it back to free memory. Reading the case runs it again for the value""")
        @Nullable Boolean actualLazy,

        @Parameter(description = "Status of the assertion execution")
        TestStatus status
) {
}
