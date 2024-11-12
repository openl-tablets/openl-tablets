package org.openl.security.acl.permission;

import java.util.List;

import org.springframework.security.acls.model.Permission;

public final class AclPermissionsSets {

    public static final List<Permission> NEW_PROJECT_PERMISSIONS = List.of(AclPermission.READ,
            AclPermission.CREATE,
            AclPermission.WRITE,
            AclPermission.DELETE);

    public static final List<Permission> NEW_DEPLOYMENT_CONFIGURATION_PERMISSIONS = List.of(AclPermission.READ,
            AclPermission.CREATE,
            AclPermission.WRITE,
            AclPermission.DELETE);

    public static final List<Permission> NEW_FILE_PERMISSIONS = List
            .of(AclPermission.READ, AclPermission.WRITE, AclPermission.DELETE);

    private AclPermissionsSets() {
    }
}
