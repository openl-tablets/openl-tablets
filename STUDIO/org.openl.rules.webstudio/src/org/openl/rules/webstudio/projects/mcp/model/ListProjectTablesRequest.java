package org.openl.rules.webstudio.projects.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

public record ListProjectTablesRequest(
        @McpToolParam(description = "Project identifier")
        String projectId,
        String name
) {
}
