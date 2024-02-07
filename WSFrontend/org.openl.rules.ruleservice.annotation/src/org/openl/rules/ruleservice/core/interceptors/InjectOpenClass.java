package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.openl.types.IOpenClass;

/**
 * This annotation is designed to inject @{@link IOpenClass} related to compiled project to ruleservice interceptors.
 *
 * @deprecated use @Autowire IOpenClass openClass
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Deprecated
@Autowired
@Qualifier("openClass")
public @interface InjectOpenClass {
}
