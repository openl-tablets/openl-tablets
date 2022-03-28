package org.openl.rules.rest.resolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Parameter(description = "Unique Identifier of target Design Repository", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
public @interface DesignRepository {

    /**
     * The name of the path variable to bind to
     */
    @AliasFor(attribute = "name", annotation = Parameter.class)
    String value() default "";

}
