package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

import org.openl.rules.project.abstraction.ProjectStatus;

public record ListProjectsRequest(
        @ToolParam(description = "Project status to filter by", required = false)
        ProjectStatus status,
        @ToolParam(description = "Repository identifier to filter by", required = false)
        String repository
) {
}
