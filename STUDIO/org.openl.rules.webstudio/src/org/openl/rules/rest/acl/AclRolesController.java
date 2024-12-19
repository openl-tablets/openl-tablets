package org.openl.rules.rest.acl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.acl.model.AclRoleModel;
import org.openl.security.acl.permission.AclRole;

@RestController
@RequestMapping(value = "/acls/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ACL Management: Roles", description = "ACL Management API for Roles")
public class AclRolesController {

    @Operation(description = "acls.get-roles.desc", summary = "acls.get-roles.summary")
    @GetMapping
    public List<AclRoleModel> getRoles() {
        return Stream.of(AclRole.values())
                .sorted(Comparator.comparing(AclRole::ordinal).reversed())
                .map(role -> new AclRoleModel(role, role.getDescription()))
                .collect(Collectors.toUnmodifiableList());
    }

}
