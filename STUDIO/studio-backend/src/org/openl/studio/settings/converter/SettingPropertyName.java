package org.openl.studio.settings.converter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JacksonAnnotationsInside
@JacksonAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JsonSerialize(using = SettingValueWrapperSerializer.class)
public @interface SettingPropertyName {

    String value() default "";

    String suffix() default "";

    boolean secret() default false;

}
