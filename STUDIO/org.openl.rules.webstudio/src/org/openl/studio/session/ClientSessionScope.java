package org.openl.studio.session;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;

/**
 * Puts a bean in the scope of one client of OpenL Studio: a browser's HTTP session, or the credentials a
 * request carries on its own.
 *
 * <p>Used where {@code @SessionScope} would be. A browser keeps its bean in its HTTP session, as before. A
 * request with a bearer token, a personal access token or Basic authentication gets the bean kept for that
 * credential, so the next request with it finds the same bean rather than a new one.
 *
 * @see ClientSessions
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(ClientSessions.NAME)
public @interface ClientSessionScope {

    /**
     * Whether the bean is injected as a proxy that finds the client's instance on every call.
     *
     * <p>Defaults to a class-based proxy, as {@code @SessionScope} does.
     */
    @AliasFor(annotation = Scope.class)
    ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
}
