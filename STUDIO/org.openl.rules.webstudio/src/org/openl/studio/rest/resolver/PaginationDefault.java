package org.openl.studio.rest.resolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defaults for {@link org.openl.rules.repository.api.Pageable}
 *
 * @author Vladyslav pikus
 * @see org.openl.rules.repository.api.Pageable
 * @see PaginationValueArgumentResolver
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PaginationDefault {

    /**
     * Default page size
     */
    int size() default 20;

    /**
     * Default page number
     */
    int page() default 0;

    /**
     * Default page offset
     */
    int offset() default 0;

}
