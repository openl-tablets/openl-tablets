package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record ListProjectTablesRequest(
        @ToolParam(description = "Project identifier")
        String projectId,
        String name
) {
}
