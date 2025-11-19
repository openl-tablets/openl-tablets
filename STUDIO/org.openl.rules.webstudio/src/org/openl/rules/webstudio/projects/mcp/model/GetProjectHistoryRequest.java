package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record GetProjectHistoryRequest(
        @ToolParam(description = "Project identifier (base64 encoded project coordinates)")
        String projectId) {
}
