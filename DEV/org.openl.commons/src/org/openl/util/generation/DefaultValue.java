package org.openl.util.generation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {
    public static final String DEFAULT = "_DEFAULT_";

    String value() default DEFAULT;
}
