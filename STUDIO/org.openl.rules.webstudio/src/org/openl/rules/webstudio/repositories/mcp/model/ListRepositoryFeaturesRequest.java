package org.openl.rules.webstudio.repositories.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

public record ListRepositoryFeaturesRequest(
        @McpToolParam(description = "Repository identifier")
        String repoId) {
}
