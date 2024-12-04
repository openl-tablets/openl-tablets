package org.openl.rules.rest.acl.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.security.acl.permission.AclRole;

public class SetAclRoleModel {

    @NotNull
    @Parameter(description = "Role to set", required = true)
    private final AclRole role;

    @JsonCreator
    public SetAclRoleModel(@JsonProperty("role") AclRole role) {
        this.role = role;
    }

    public AclRole getRole() {
        return role;
    }

}
