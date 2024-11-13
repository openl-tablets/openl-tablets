package org.openl.security.acl.permission;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.security.acls.domain.CumulativePermission;
import org.springframework.security.acls.model.Permission;

public enum AclRole {

    /**
     * A role designed for the administration and oversight of a specific resource within the system.
     */
    MANAGER(AclPermission.ADMINISTRATION,
            AclPermission.READ,
            AclPermission.CREATE,
            AclPermission.WRITE,
            AclPermission.DELETE),

    /**
     * A role with permissions to read and modify content within the resource.
     * Their role includes updating, creating, and deleting content or data, as well as making changes
     * to existing information. Editors can modify the resources they have access to but do not have access
     * to the alter system settings or manage user permissions.
     */
    CONTRIBUTOR(AclPermission.READ,
            AclPermission.CREATE,
            AclPermission.WRITE,
            AclPermission.DELETE),

    /**
     * A role with read-only access to the specific resource. They can view content, data, or settings run tests
     * but cannot make any changes, modifications, or deletions. Their role is to monitor and review information
     * without affecting the integrity or configuration of the resource.
     */
    VIEWER(AclPermission.READ);

    private final CumulativePermission cumulativePermission;

    AclRole(Permission... permissions) {
        this.cumulativePermission = Stream.of(permissions)
                .collect(CumulativePermission::new,
                        CumulativePermission::set,
                        (left, right) -> {
                            throw new UnsupportedOperationException();
                        });
    }

    public CumulativePermission getCumulativePermission() {
        return cumulativePermission;
    }

    public int getMask() {
        return cumulativePermission.getMask();
    }

    public static AclRole getRole(int mask) {
        if (MANAGER.getMask() == mask) {
            return MANAGER;
        } else if (CONTRIBUTOR.getMask() == mask) {
            return CONTRIBUTOR;
        } else if (VIEWER.getMask() == mask) {
            return VIEWER;
        } else {
            return null;
        }
    }

    public List<Permission> getPermissions() {
        List<Permission> basePermissions = new ArrayList<>();
        for (var basePermission : AclPermission.ALL_SUPPORTED_PERMISSIONS) {
            if ((cumulativePermission.getMask() & basePermission.getMask()) != 0) {
                basePermissions.add(basePermission);
            }
        }
        return Collections.unmodifiableList(basePermissions);
    }

}
