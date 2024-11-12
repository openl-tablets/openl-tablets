package org.openl.security.acl.permission;

import java.util.Collection;
import java.util.List;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

public class AclPermission extends BasePermission {

    public static final Collection<Permission> ALL_SUPPORTED_DESIGN_REPO_PERMISSIONS = List
            .of(READ, WRITE, CREATE, DELETE);

    public static final Collection<Permission> ALL_SUPPORTED_DEPLOY_CONFIG_REPO_PERMISSIONS = List
            .of(READ, WRITE, CREATE, DELETE);

    public static final Collection<Permission> ALL_SUPPORTED_PROD_REPO_PERMISSIONS = List.of(READ, WRITE, DELETE);

    protected AclPermission(int mask) {
        super(mask);
    }

    protected AclPermission(int mask, char code) {
        super(mask, code);
    }

    public static String toString(Permission permission) {
        if (READ.getMask() == permission.getMask()) {
            return "VIEW";
        } else if (CREATE.getMask() == permission.getMask()) {
            return "CREATE";
        } else if (WRITE.getMask() == permission.getMask()) {
            return "EDIT";
        } else if (DELETE.getMask() == permission.getMask()) {
            return "DELETE";
        }
        return null;
    }

    public static Permission getPermission(String permission) {
        switch (permission) {
            case "VIEW":
                return READ;
            case "CREATE":
                return CREATE;
            case "EDIT":
                return WRITE;
            case "DELETE":
                return DELETE;
            default:
                return null;
        }
    }

    public static Permission getPermission(int mask) {
        if (READ.getMask() == mask) {
            return READ;
        } else if (CREATE.getMask() == mask) {
            return CREATE;
        } else if (WRITE.getMask() == mask) {
            return WRITE;
        } else if (DELETE.getMask() == mask) {
            return DELETE;
        }
        return null;
    }
}
