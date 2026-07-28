package org.openl.rules.rest.acl.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

import org.openl.security.acl.permission.AclRole;

@RequiredArgsConstructor
@Schema(description = "ACL Role Model")
public class AclRoleModel {

    @Parameter(description = "Code", required = true)
    private final AclRole code;

    @Parameter(description = "Description", required = true)
    private final String description;

    public AclRole getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
