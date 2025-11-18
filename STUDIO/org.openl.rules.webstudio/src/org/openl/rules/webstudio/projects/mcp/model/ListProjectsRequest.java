package org.openl.rules.webstudio.projects.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

import org.openl.rules.project.abstraction.ProjectStatus;

public record ListProjectsRequest(
        @McpToolParam(description = "Project status to filter by", required = false)
        ProjectStatus status,
        @McpToolParam(description = "Repository identifier to filter by", required = false)
        String repository
) {
}
