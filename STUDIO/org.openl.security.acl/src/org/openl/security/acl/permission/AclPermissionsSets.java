package org.openl.security.acl.permission;

import java.util.List;

import org.springframework.security.acls.model.Permission;

public final class AclPermissionsSets {

    public static final List<Permission> NEW_PROJECT_PERMISSIONS = List.of(AclPermission.VIEW,
        AclPermission.EDIT,
        AclPermission.ARCHIVE,
        AclPermission.DELETE,
        AclPermission.RUN,
        AclPermission.BENCHMARK,
        AclPermission.CREATE_PROJECTS,
        AclPermission.CREATE_TABLES,
        AclPermission.EDIT_TABLES,
        AclPermission.DELETE_TABLES);

    private AclPermissionsSets() {
    }
}
