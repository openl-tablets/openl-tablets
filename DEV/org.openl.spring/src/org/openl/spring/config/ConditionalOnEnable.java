package org.openl.spring.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Enables bean registration when all properties not "false" and not empty. For example in
 * {@code @ConditionalOnEnable({"feature.enabled", "feature.module.enabled", "feature.param"}) }
 * bean registration will be when the following properties are defined:
 * 
 * <pre>
 *     feature.enabled=true
 *     feature.module.enabled=true
 *     feature.param=10
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
     * The set of properties to check.
     */
    String[] value();
}
