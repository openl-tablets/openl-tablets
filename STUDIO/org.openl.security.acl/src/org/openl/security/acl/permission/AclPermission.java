package org.openl.security.acl.permission;

import java.util.Collection;
import java.util.List;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

public class AclPermission extends BasePermission {

    public static final int MASK_END = 24;

    public static final Permission DESIGN_REPOSITORY_READ = new AclPermission(1, 'R');
    public static final Permission DESIGN_REPOSITORY_WRITE = new AclPermission(1 << 2, 'W');
    public static final Permission DESIGN_REPOSITORY_DELETE = new AclPermission(1 << 3, 'D');
    public static final Permission DESIGN_REPOSITORY_CREATE = new AclPermission(1 << 4, 'C');

    public static final Permission VIEW = new AclPermission(1 << MASK_END | DESIGN_REPOSITORY_READ.getMask(), 'V');
    public static final Permission CREATE = new AclPermission(2 << MASK_END | DESIGN_REPOSITORY_CREATE.getMask(),
            'N');
    public static final Permission EDIT = new AclPermission(
            4 << MASK_END | DESIGN_REPOSITORY_WRITE.getMask() | DESIGN_REPOSITORY_CREATE.getMask(),
            'E');

    public static final Permission DELETE = new AclPermission(5 << MASK_END | DESIGN_REPOSITORY_DELETE.getMask(),
            'A');

    public static final Collection<Permission> ALL_SUPPORTED_DESIGN_REPO_PERMISSIONS = List
            .of(VIEW, EDIT, CREATE, DELETE);

    public static final Collection<Permission> ALL_SUPPORTED_DEPLOY_CONFIG_REPO_PERMISSIONS = List
            .of(VIEW, EDIT, CREATE, DELETE);

    public static final Collection<Permission> ALL_SUPPORTED_PROD_REPO_PERMISSIONS = List.of(VIEW, EDIT, DELETE);

    protected AclPermission(int mask) {
        super(mask);
    }

    protected AclPermission(int mask, char code) {
        super(mask, code);
    }

    public static String toString(Permission permission) {
        if (VIEW.getMask() == permission.getMask()) {
            return "VIEW";
        } else if (CREATE.getMask() == permission.getMask()) {
            return "CREATE";
        } else if (EDIT.getMask() == permission.getMask()) {
            return "EDIT";
        } else if (DELETE.getMask() == permission.getMask()) {
            return "DELETE";
        }
        return null;
    }

    public static Permission getPermission(String permission) {
        switch (permission) {
            case "VIEW":
                return VIEW;
            case "CREATE":
                return CREATE;
            case "EDIT":
                return EDIT;
            case "DELETE":
                return DELETE;
            default:
                return null;
        }
    }

    public static Permission getPermission(int mask) {
        if (VIEW.getMask() == mask) {
            return VIEW;
        } else if (CREATE.getMask() == mask) {
            return CREATE;
        } else if (EDIT.getMask() == mask) {
            return EDIT;
        } else if (DELETE.getMask() == mask) {
            return DELETE;
        }
        return null;
    }
}
