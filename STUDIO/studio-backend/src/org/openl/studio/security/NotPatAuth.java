package org.openl.studio.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Custom security annotation to restrict access for Personal Access Token (PAT) authenticated users.
 * <p>
 * This annotation ensures that the annotated method can only be accessed by users who are not
 * authenticated via a Personal Access Token (PAT). It utilizes Spring Expression Language (SpEL)
 * to delegate the check to the {@code authz} component's {@code isNotPat} method.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * @NotPatAuth
 * public void performAction() {
 *     // Logic accessible only to non-PAT authenticated users
 * }
 * }</pre>
 *
 * @author Vladyslav Pikus
 * @see org.springframework.security.access.prepost.PreAuthorize
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@authz.isNotPat(authentication)")
public @interface NotPatAuth {
}
