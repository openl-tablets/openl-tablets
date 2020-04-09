package org.openl.rules.webstudio.web.jsf.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;

/**
 * Acts as a shortcut for {@code @Scope("view")} with the default {@link #proxyMode} set to
 * {@link ScopedProxyMode#TARGET_CLASS TARGET_CLASS}
 */
@Scope(value = "view")
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewScope {
    /**
     * Alias for {@link Scope#proxyMode}.
     * <p>
     * Defaults to {@link ScopedProxyMode#TARGET_CLASS}.
     */
    @AliasFor(annotation = Scope.class)
    ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
}
