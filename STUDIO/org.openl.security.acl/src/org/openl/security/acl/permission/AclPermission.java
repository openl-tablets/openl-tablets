package org.openl.security.acl.permission;

import org.springframework.security.acls.domain.BasePermission;

public class AclPermission extends BasePermission {

    public static final int MASK_END = 24;

    public static final AclPermission DESIGN_REPOSITORY_READ = new AclPermission(1, 'R');
    public static final AclPermission DESIGN_REPOSITORY_WRITE = new AclPermission(1 << 2, 'W');
    public static final AclPermission DESIGN_REPOSITORY_DELETE = new AclPermission(1 << 3, 'D');
    public static final AclPermission DESIGN_REPOSITORY_CREATE = new AclPermission(1 << 4, 'C');

    public static final AclPermission VIEW = new AclPermission(1 << MASK_END | DESIGN_REPOSITORY_READ.getMask(), 'V');
    public static final AclPermission CREATE_PROJECTS = new AclPermission(
        2 << MASK_END | DESIGN_REPOSITORY_CREATE.getMask(),
        'C');
    public static final AclPermission CREATE_MODULES = new AclPermission(
        3 << MASK_END | DESIGN_REPOSITORY_CREATE.getMask(),
        'M');
    public static final AclPermission EDIT = new AclPermission(4 << MASK_END | DESIGN_REPOSITORY_WRITE.getMask(), 'W');
    public static final AclPermission ARCHIVE = new AclPermission(5 << MASK_END | DESIGN_REPOSITORY_WRITE.getMask(),
        'A');
    public static final AclPermission DELETE = new AclPermission(6 << MASK_END | DESIGN_REPOSITORY_DELETE.getMask(),
        'D');

    public static final AclPermission CREATE_DEPLOYMENT = new AclPermission(
        7 << MASK_END | DESIGN_REPOSITORY_CREATE.getMask(),
        'Y');
    public static final AclPermission EDIT_DEPLOYMENT = new AclPermission(
        8 << MASK_END | DESIGN_REPOSITORY_WRITE.getMask(),
        'Y');
    public static final AclPermission ARCHIVE_DEPLOYMENT = new AclPermission(
        9 << MASK_END | DESIGN_REPOSITORY_WRITE.getMask(),
        'Y');
    public static final AclPermission DELETE_DEPLOYMENT = new AclPermission(
        10 << MASK_END | DESIGN_REPOSITORY_DELETE.getMask(),
        'Y');

    public static final AclPermission DEPLOY_PROJECT = new AclPermission(11 << MASK_END, 'Y');

    public static final AclPermission RUN = new AclPermission(12 << MASK_END, 'X');
    public static final AclPermission BENCHMARK = new AclPermission(13 << MASK_END, 'X');

    public static final AclPermission CREATE_TABLES = new AclPermission(14 << MASK_END, 'X');
    public static final AclPermission EDIT_TABLES = new AclPermission(15 << MASK_END, 'X');
    public static final AclPermission DELETE_TABLES = new AclPermission(16 << MASK_END, 'X');

    protected AclPermission(int mask) {
        super(mask);
    }

    protected AclPermission(int mask, char code) {
        super(mask, code);
    }

    public static String toString(AclPermission permission) {
        if (VIEW.getMask() == permission.getMask()) {
            return "VIEW";
        } else if (CREATE_PROJECTS.getMask() == permission.getMask()) {
            return "CREATE_PROJECTS";
        } else if (CREATE_MODULES.getMask() == permission.getMask()) {
            return "CREATE_MODULES";
        } else if (EDIT.getMask() == permission.getMask()) {
            return "EDIT";
        } else if (ARCHIVE.getMask() == permission.getMask()) {
            return "ARCHIVE";
        } else if (DELETE.getMask() == permission.getMask()) {
            return "DELETE";
        } else if (DEPLOY_PROJECT.getMask() == permission.getMask()) {
            return "DEPLOY_PROJECT";
        } else if (RUN.getMask() == permission.getMask()) {
            return "RUN";
        } else if (BENCHMARK.getMask() == permission.getMask()) {
            return "BENCHMARK";
        } else if (CREATE_TABLES.getMask() == permission.getMask()) {
            return "CREATE_TABLES";
        } else if (EDIT_TABLES.getMask() == permission.getMask()) {
            return "EDIT_TABLES";
        } else if (DELETE_TABLES.getMask() == permission.getMask()) {
            return "DELETE_TABLES";
        } else if (CREATE_DEPLOYMENT.getMask() == permission.getMask()) {
            return "CREATE_DEPLOYMENT";
        } else if (EDIT_DEPLOYMENT.getMask() == permission.getMask()) {
            return "EDIT_DEPLOYMENT";
        } else if (ARCHIVE_DEPLOYMENT.getMask() == permission.getMask()) {
            return "ARCHIVE_DEPLOYMENT";
        } else if (DELETE_DEPLOYMENT.getMask() == permission.getMask()) {
            return "DELETE_DEPLOYMENT";
        }
        return null;
    }

    public static AclPermission getPermission(String permission) {
        switch (permission) {
            case "VIEW":
                return VIEW;
            case "CREATE_PROJECTS":
                return CREATE_PROJECTS;
            case "CREATE_MODULES":
                return CREATE_MODULES;
            case "EDIT":
                return EDIT;
            case "ARCHIVE":
                return ARCHIVE;
            case "DELETE":
                return DELETE;
            case "DEPLOY_PROJECT":
                return DEPLOY_PROJECT;
            case "RUN":
                return RUN;
            case "BENCHMARK":
                return BENCHMARK;
            case "CREATE_TABLES":
                return CREATE_TABLES;
            case "EDIT_TABLES":
                return EDIT_TABLES;
            case "DELETE_TABLES":
                return DELETE_TABLES;
            case "CREATE_DEPLOYMENT":
                return CREATE_DEPLOYMENT;
            case "EDIT_DEPLOYMENT":
                return EDIT_DEPLOYMENT;
            case "ARCHIVE_DEPLOYMENT":
                return ARCHIVE_DEPLOYMENT;
            case "DELETE_DEPLOYMENT":
                return DELETE_DEPLOYMENT;
            default:
                return null;
        }
    }

    public static AclPermission getPermission(int mask) {
        if (VIEW.getMask() == mask) {
            return VIEW;
        } else if (CREATE_PROJECTS.getMask() == mask) {
            return CREATE_PROJECTS;
        } else if (CREATE_MODULES.getMask() == mask) {
            return CREATE_MODULES;
        } else if (EDIT.getMask() == mask) {
            return EDIT;
        } else if (ARCHIVE.getMask() == mask) {
            return ARCHIVE;
        } else if (DELETE.getMask() == mask) {
            return DELETE;
        } else if (DEPLOY_PROJECT.getMask() == mask) {
            return DEPLOY_PROJECT;
        } else if (RUN.getMask() == mask) {
            return RUN;
        } else if (BENCHMARK.getMask() == mask) {
            return BENCHMARK;
        } else if (CREATE_TABLES.getMask() == mask) {
            return CREATE_TABLES;
        } else if (EDIT_TABLES.getMask() == mask) {
            return EDIT_TABLES;
        } else if (DELETE_TABLES.getMask() == mask) {
            return DELETE_TABLES;
        } else if (CREATE_DEPLOYMENT.getMask() == mask) {
            return CREATE_DEPLOYMENT;
        } else if (EDIT_DEPLOYMENT.getMask() == mask) {
            return EDIT_DEPLOYMENT;
        } else if (ARCHIVE_DEPLOYMENT.getMask() == mask) {
            return ARCHIVE_DEPLOYMENT;
        } else if (DELETE_DEPLOYMENT.getMask() == mask) {
            return DELETE_DEPLOYMENT;
        }
        return null;
    }
}
