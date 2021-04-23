package org.openl.spring.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Enables bean registration when all properties are true. For example in {@code @Conditional({"feature.enabled",
 * "feature.module.enabled"}) } bean registration will be when the following properties are defined:
 * 
 * <pre>
 *     feature.enabled=true
 *     feature.module.enabled=true
 * </pre>
 * 
 * @author Yury Molchan
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(EnableCondition.class)
public @interface ConditionalOnEnable {

    /**
     * The set of properties with {@code "true"} values.
     */
    String[] value();
}
