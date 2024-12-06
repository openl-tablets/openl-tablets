package org.openl.rules.rest.acl.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.security.acl.permission.AclRole;

@Schema(description = "ACL Role Model")
public class AclRoleModel {

    @Parameter(description = "Code", required = true)
    private final AclRole code;

    @Parameter(description = "Description", required = true)
    private final String description;

    public AclRoleModel(AclRole code, String description) {
        this.code = code;
        this.description = description;
    }

    public AclRole getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
