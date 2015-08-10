package org.openl.util.generation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {
    String value() default "_DEFAULT_";
}
