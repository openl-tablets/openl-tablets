package org.openl.studio.projects.model.tests;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import lombok.Singular;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.TestStatus;

@Builder
public record TestUnitExecutionResult(
        @Parameter(description = "Identifier of the test unit")
        String id,

        @Parameter(description = "Description of the test unit")
        String description,

        @Parameter(description = "Execution time of the test unit in milliseconds")
        double executionTimeMs,

        @Parameter(description = "Status of the test unit execution")
        TestStatus status,

        @Parameter(description = "List of test assertion execution results")
        @Singular("testAssertion")
        List<TestAssertionExecutionResult> testAssertions,

        @Parameter(description = "List of test parameter values")
        @Singular("parameter")
        List<TestParameterValue> parameters,

        @Parameter(description = "List of context parameter values")
        @Singular("contextParameter")
        List<TestParameterValue> contextParameters,

        @Parameter(description = "List of error messages occurred during test unit execution")
        @Singular("error")
        List<MessageDescription> errors
) {
}
