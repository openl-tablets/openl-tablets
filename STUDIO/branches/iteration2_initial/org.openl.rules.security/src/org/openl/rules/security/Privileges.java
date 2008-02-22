package org.openl.rules.security;

/**
 * Constants for all Roles.
 * <p>
 * It is possible to use {@link #ROLE_PREFIX} to define other constants like <code>ROLE_PREFIX + "ADMIN"</code>,
 * but it'll affect full text search.
 * </p>
 *
 * @author Aleh Bykhavets
 */
public class Privileges {
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String PRIVILEGE_DEPLOY = "DEPLOY_PROJECTS";
    public static final String PRIVILEGE_EDIT = "EDIT_PROJECTS";
    public static final String PRIVILEGE_VIEW = "VIEW_PROJECTS";
    public static final String PRIVILEGE_ERASE = "ERASE_PROJECTS";
}
