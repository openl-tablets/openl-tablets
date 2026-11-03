package org.openl.studio.common.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Registers a component or a bean method only when an expression is {@code true}.
 *
 * <p>The expression is SpEL. Property placeholders such as {@code ${user.mode}} are replaced by the property values
 * first, so {@code "'${user.mode}' != 'single'"} compares the current user mode. A placeholder of an absent property
 * stays as written.
 *
 * <p>Only literals and operators are evaluated: the expression reaches no bean, type or method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(OnExpressionCondition.class)
public @interface ConditionalOnExpression {

    /**
     * The expression that decides whether the component is registered.
     */
    String value();
}
