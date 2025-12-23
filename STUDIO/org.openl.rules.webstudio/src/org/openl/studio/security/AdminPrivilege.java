package org.openl.studio.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Custom security annotation to restrict access to methods requiring administrator privileges.
 * <p>
 * This annotation enforces that only users with the {@code ADMIN} privilege are allowed to access
 * the annotated method. It utilizes Spring Expression Language (SpEL) to delegate the privilege
 * check to the {@code accessManager}.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * @AdminPrivilege
 * public void performAdminAction() {
 *     // Logic accessible only to ADMIN users
 * }
 * }</pre>
 *
 * @author Vladyslav Pikus
 * @see org.springframework.security.access.prepost.PreAuthorize
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("""
        hasAuthority(T(org.openl.rules.security.Privileges).ADMIN.getAuthority())
        and @authz.isNotPat(authentication)
        """)
public @interface AdminPrivilege {
}
