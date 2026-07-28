package org.openl.rules.rest.acl.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.security.acl.permission.AclRole;

@RequiredArgsConstructor
@Schema(description = "ACL Role Model")
public class AclRoleModel {

    @Getter
    @Parameter(description = "Code", required = true)
    private final AclRole code;

    @Getter
    @Parameter(description = "Description", required = true)
    private final String description;

}
