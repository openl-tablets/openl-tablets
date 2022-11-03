package org.openl.security.acl.permission;

import java.util.List;

import org.springframework.security.acls.model.Permission;

public final class AclPermissionsSets {

    public static final List<Permission> NEW_PROJECT_PERMISSIONS = List.of(AclPermission.VIEW,
        AclPermission.APPEND,
        AclPermission.EDIT,
        AclPermission.ARCHIVE,
        AclPermission.DELETE,
        AclPermission.RUN,
        AclPermission.BENCHMARK,
        AclPermission.CREATE_TABLES,
        AclPermission.EDIT_TABLES,
        AclPermission.DELETE_TABLES);

    public static final List<Permission> NEW_FILE_PERMISSIONS = List.of(AclPermission.EDIT, AclPermission.DELETE);

    private AclPermissionsSets() {
    }
}
