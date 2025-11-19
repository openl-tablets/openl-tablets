package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record RunProjectTestsRequest(
        @ToolParam(description = "Base64-encoded project identifier")
        String projectId,
        @ToolParam(description = "Module name to run tests from (optional, if not provided runs all)", required = false)
        String fromModule,
        @ToolParam(description = "Table ID to run tests for a specific table (optional)", required = false)
        String tableId,
        @ToolParam(description = "Test ranges to run specific tests in the table, e.g., '1:5,10:15' (optional)", required = false)
        String testRanges
) {
}
