package org.openl.rules.webstudio.repositories.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

import org.openl.security.acl.repository.AclRepositoryType;

public record ListRepositoriesRequest(
        @McpToolParam(description = "Repository type")
        AclRepositoryType type) {
}
