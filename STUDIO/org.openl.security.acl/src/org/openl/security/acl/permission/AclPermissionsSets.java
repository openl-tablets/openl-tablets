package org.openl.security.acl.permission;

import java.util.List;

import org.springframework.security.acls.model.Permission;

public final class AclPermissionsSets {

    public static final List<Permission> NEW_PROJECT_PERMISSIONS = List.of(AclPermission.VIEW,
        AclPermission.ADD,
        AclPermission.EDIT,
        AclPermission.DELETE,
        AclPermission.ERASE,
        AclPermission.RUN,
        AclPermission.BENCHMARK);

    public static final List<Permission> NEW_DEPLOYMENT_CONFIGURATION_PERMISSIONS = List.of(AclPermission.VIEW,
        AclPermission.ADD,
        AclPermission.EDIT,
        AclPermission.DELETE,
        AclPermission.ERASE,
        AclPermission.DEPLOY);

    public static final List<Permission> NEW_FILE_PERMISSIONS = List
        .of(AclPermission.VIEW, AclPermission.EDIT, AclPermission.DELETE);

    private AclPermissionsSets() {
    }
}
