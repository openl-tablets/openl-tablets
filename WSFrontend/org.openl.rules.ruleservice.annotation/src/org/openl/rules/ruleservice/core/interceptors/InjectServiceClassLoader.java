package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * This annotation is designed to inject @{@link org.openl.classloader.OpenLClassLoader} related to compiled service to
 * ruleservice interceptors.
 *
 * @deprecated use @Autowire ClassLoader serviceClassLoader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Deprecated
@Autowired
@Qualifier("serviceClassLoader")
public @interface InjectServiceClassLoader {
}
