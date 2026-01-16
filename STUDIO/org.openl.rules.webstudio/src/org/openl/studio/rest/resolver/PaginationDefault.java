package org.openl.studio.rest.resolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defaults for {@link org.openl.rules.repository.api.Pageable}
 *
 * @author Vladyslav pikus
 * @see org.openl.rules.repository.api.Pageable
 * @see PaginationValueArgumentResolver
 */
@Parameter(name = "offset",
        description = "pagination.param.offset.desc",
        in = ParameterIn.QUERY,
        schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"))
@Parameter(name = "page",
        description = "pagination.param.page.desc",
        in = ParameterIn.QUERY,
        schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"))
@Parameter(name = "size",
        description = "pagination.param.size.desc",
        in = ParameterIn.QUERY,
        schema = @Schema(type = "integer", format = "int32", minimum = "1", defaultValue = "50"))
@Parameter(name = "unpaged",
        description = "pagination.param.unpaged.desc",
        in = ParameterIn.QUERY,
        schema = @Schema(type = "boolean", defaultValue = "false"))
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PaginationDefault {

    /**
     * Default page size
     */
    int size() default 50;

    /**
     * Default page number
     */
    int page() default 0;

    /**
     * Default page offset
     */
    int offset() default 0;

}
