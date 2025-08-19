package org.openl.studio.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Custom security annotation to enforce access control.
 * <p>
 * This annotation ensures that the currently authenticated user can access the
 * resource if they are either:
 * <ul>
 *     <li>The owner of the resource (matching username with {@code authentication.name}).</li>
 *     <li>Granted the {@code ADMIN} privilege.</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * @OwnerOrAdminPrivilege
 * public void updateResource(String username) {
 *     // Business logic here
 * }
 * }</pre>
 *
 * @author Vladyslav Pikus
 * @see org.springframework.security.access.prepost.PreAuthorize
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("#username == authentication.name or hasAuthority(T(org.openl.rules.security.Privileges).ADMIN.getAuthority())")
public @interface OwnerOrAdminPrivilege {
}
