package org.openl.rules.webstudio.repositories.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

import org.openl.security.acl.repository.AclRepositoryType;

public record ListRepositoriesRequest(
        @ToolParam(description = "Repository type")
        AclRepositoryType type) {
}
