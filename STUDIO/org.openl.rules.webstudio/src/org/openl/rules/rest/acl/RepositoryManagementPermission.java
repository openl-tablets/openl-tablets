package org.openl.rules.rest.acl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Annotation to indicate that a method can manage a repository.
 * <p>
 * This annotation uses Spring Security's @PreAuthorize to check if the user has
 * the ADMIN authority or if the user has ADMINISTRATION permission on the specified ACL repository.
 * <p>
 * The annotation can be applied to methods.
 * @see PreAuthorize
 * @see org.openl.rules.rest.acl.service.AclRepositoriesHelper
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority(T(org.openl.rules.security.Privileges).ADMIN.getAuthority()) or @aclRepositoriesHelper.hasPermission(#aclRepoId, T(org.springframework.security.acls.domain.BasePermission).ADMINISTRATION)")
public @interface RepositoryManagementPermission {
}
