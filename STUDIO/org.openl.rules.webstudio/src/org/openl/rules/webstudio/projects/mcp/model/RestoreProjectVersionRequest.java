package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record RestoreProjectVersionRequest(
        @ToolParam(description = "Project identifier (base64 encoded project coordinates)")
        String projectId,
        @ToolParam(description = "Version ID to restore (timestamp or version name)")
        String versionId) {
}
