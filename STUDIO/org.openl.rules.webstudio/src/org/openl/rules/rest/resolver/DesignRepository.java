package org.openl.rules.rest.resolver;

import java.lang.annotation.*;

/**
 * Annotation which indicates that a method parameter should be bound to a URI template variable.
 *
 * Assigned value type must be assignable to {@link org.openl.rules.repository.api.Repository}
 *
 * @author Vladyslav Pikus
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DesignRepository {

    /**
     * The name of the path variable to bind to
     */
    String value() default "";

}
