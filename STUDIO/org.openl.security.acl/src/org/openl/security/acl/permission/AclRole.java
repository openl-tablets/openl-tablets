package org.openl.security.acl.permission;


import java.util.stream.Stream;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.CumulativePermission;
import org.springframework.security.acls.model.Permission;

public enum AclRole {

    /**
     * A role designed for the administration and oversight of a specific resource within the system.
     */
    MANAGER("Manager",
            BasePermission.ADMINISTRATION,
            BasePermission.READ,
            BasePermission.CREATE,
            BasePermission.WRITE,
            BasePermission.DELETE),

    /**
     * A role with permissions to read and modify content within the resource.
     * Their role includes updating, creating, and deleting content or data, as well as making changes
     * to existing information. Editors can modify the resources they have access to but do not have access
     * to the alter system settings or manage user permissions.
     */
    CONTRIBUTOR("Contributor",
            BasePermission.READ,
            BasePermission.CREATE,
            BasePermission.WRITE,
            BasePermission.DELETE),

    /**
     * A role with read-only access to the specific resource. They can view content, data, or settings run tests
     * but cannot make any changes, modifications, or deletions. Their role is to monitor and review information
     * without affecting the integrity or configuration of the resource.
     */
    VIEWER("Viewer", BasePermission.READ);

    private final String description;
    private final CumulativePermission cumulativePermission;

    AclRole(String description, Permission... permissions) {
        this.description = description;
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

    public String getDescription() {
        return description;
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

}
