package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record GetProjectTestsSummaryRequest(
        @ToolParam(description = "Base64-encoded project identifier", required = true)
        String projectId
) {
}
