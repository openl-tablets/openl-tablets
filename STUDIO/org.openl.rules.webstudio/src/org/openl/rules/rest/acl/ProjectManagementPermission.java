package org.openl.rules.rest.acl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Annotation to indicate that a method can manage a projects.
 * <p>
 * This annotation uses Spring Security's @PreAuthorize to check if the user has
 * the ADMIN authority or if the user has ADMINISTRATION permission on the specified ACL project.
 * <p>
 * The annotation can be applied to methods.
 * @see PreAuthorize
 * @see org.openl.rules.rest.acl.service.AclProjectsHelper
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority(T(org.openl.rules.security.Privileges).ADMIN.getAuthority()) or @aclProjectsHelper.hasPermission(#project, T(org.openl.security.acl.permission.AclPermission).ADMINISTRATION)")
public @interface ProjectManagementPermission {
}
