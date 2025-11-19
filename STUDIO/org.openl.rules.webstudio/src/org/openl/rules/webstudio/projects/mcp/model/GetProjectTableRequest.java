package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record GetProjectTableRequest(
        @ToolParam(description = "Project identifier")
        String projectId,
        @ToolParam(description = "Table identifier")
        String tableId
) {
}
