package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.project.model.RulesDeploy;

/**
 * This annotation is designed to inject @{@link RulesDeploy} related to compiled project to ruleservice interceptors.
 * If a project doesn't have @{@link RulesDeploy} then null be injected.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.METHOD })
public @interface InjectRulesDeploy {
}
