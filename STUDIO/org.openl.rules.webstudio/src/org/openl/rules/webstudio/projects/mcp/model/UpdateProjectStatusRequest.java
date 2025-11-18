package org.openl.rules.webstudio.projects.mcp.model;

import java.util.Set;

import org.springaicommunity.mcp.annotation.McpToolParam;

public record UpdateProjectStatusRequest(
        @McpToolParam(description = "Project identifier")
        String projectId,
        @McpToolParam(description = "Project status to set")
        ProjectStatusToSet status,
        @McpToolParam(description = "Branch name to switching to. Supported only if project repository supports branch feature.", required = false)
        String branch,
        @McpToolParam(description = "Revision to switch to", required = false)
        String revision,
        @McpToolParam(description = "Comment", required = false)
        String comment,
        @McpToolParam(description = "The list of selected branches", required = false)
        Set<String> selectedBranches
) {
}
