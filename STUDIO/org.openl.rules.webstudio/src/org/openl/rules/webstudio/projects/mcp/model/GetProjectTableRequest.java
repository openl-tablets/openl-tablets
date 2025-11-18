package org.openl.rules.webstudio.projects.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

public record GetProjectTableRequest(
        @McpToolParam(description = "Project identifier")
        String projectId,
        @McpToolParam(description = "Table identifier")
        String tableId
) {
}
