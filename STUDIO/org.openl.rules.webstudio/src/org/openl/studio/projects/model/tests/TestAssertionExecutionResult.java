package org.openl.studio.projects.model.tests;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

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

        @Parameter(description = "Status of the assertion execution")
        TestStatus status
) {
}
