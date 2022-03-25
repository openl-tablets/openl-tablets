package org.openl.rules.spring.openapi.app040;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(in = ParameterIn.PATH, required = true)
public @interface CustomPathVariable {

    @AliasFor(attribute = "name", annotation = Parameter.class)
    String value() default "";
}
