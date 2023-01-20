package org.openl.security.acl.permission;

import org.springframework.security.acls.domain.BasePermission;

public class AclPermission extends BasePermission {

    public static final int MASK_END = 24;

    public static final AclPermission DESIGN_REPOSITORY_READ = new AclPermission(1, 'R');
    public static final AclPermission DESIGN_REPOSITORY_WRITE = new AclPermission(1 << 2, 'W');
    public static final AclPermission DESIGN_REPOSITORY_DELETE = new AclPermission(1 << 3, 'D');
    public static final AclPermission DESIGN_REPOSITORY_CREATE = new AclPermission(1 << 4, 'C');

    public static final AclPermission VIEW = new AclPermission(1 << MASK_END | DESIGN_REPOSITORY_READ.getMask(), 'V');
    public static final AclPermission CREATE = new AclPermission(2 << MASK_END | DESIGN_REPOSITORY_CREATE.getMask(),
        'C');
    public static final AclPermission ADD = new AclPermission(3 << MASK_END | DESIGN_REPOSITORY_CREATE.getMask(), 'A');
    public static final AclPermission EDIT = new AclPermission(
        4 << MASK_END | DESIGN_REPOSITORY_WRITE.getMask() | DESIGN_REPOSITORY_CREATE
            .getMask() | DESIGN_REPOSITORY_DELETE.getMask(),
        'W');
    public static final AclPermission ARCHIVE = new AclPermission(5 << MASK_END | DESIGN_REPOSITORY_WRITE.getMask(),
        'A');
    public static final AclPermission DELETE = new AclPermission(6 << MASK_END | DESIGN_REPOSITORY_DELETE.getMask(),
        'D');

    public static final AclPermission DEPLOY = new AclPermission(11 << MASK_END, 'Y');

    public static final AclPermission RUN = new AclPermission(12 << MASK_END, 'X');
    public static final AclPermission BENCHMARK = new AclPermission(13 << MASK_END, 'X');

    protected AclPermission(int mask) {
        super(mask);
    }

    protected AclPermission(int mask, char code) {
        super(mask, code);
    }

    public static String toString(AclPermission permission) {
        if (VIEW.getMask() == permission.getMask()) {
            return "VIEW";
        } else if (CREATE.getMask() == permission.getMask()) {
            return "CREATE";
        } else if (ADD.getMask() == permission.getMask()) {
            return "ADD";
        } else if (EDIT.getMask() == permission.getMask()) {
            return "EDIT";
        } else if (ARCHIVE.getMask() == permission.getMask()) {
            return "ARCHIVE";
        } else if (DELETE.getMask() == permission.getMask()) {
            return "DELETE";
        } else if (DEPLOY.getMask() == permission.getMask()) {
            return "DEPLOY";
        } else if (RUN.getMask() == permission.getMask()) {
            return "RUN";
        } else if (BENCHMARK.getMask() == permission.getMask()) {
            return "BENCHMARK";
        }
        return null;
    }

    public static AclPermission getPermission(String permission) {
        switch (permission) {
            case "VIEW":
                return VIEW;
            case "CREATE":
                return CREATE;
            case "ADD":
                return ADD;
            case "EDIT":
                return EDIT;
            case "ARCHIVE":
                return ARCHIVE;
            case "DELETE":
                return DELETE;
            case "DEPLOY":
                return DEPLOY;
            case "RUN":
                return RUN;
            case "BENCHMARK":
                return BENCHMARK;
            default:
                return null;
        }
    }

    public static AclPermission getPermission(int mask) {
        if (VIEW.getMask() == mask) {
            return VIEW;
        } else if (CREATE.getMask() == mask) {
            return CREATE;
        } else if (ADD.getMask() == mask) {
            return ADD;
        } else if (EDIT.getMask() == mask) {
            return EDIT;
        } else if (ARCHIVE.getMask() == mask) {
            return ARCHIVE;
        } else if (DELETE.getMask() == mask) {
            return DELETE;
        } else if (DEPLOY.getMask() == mask) {
            return DEPLOY;
        } else if (RUN.getMask() == mask) {
            return RUN;
        } else if (BENCHMARK.getMask() == mask) {
            return BENCHMARK;
        }
        return null;
    }
}
