package org.openl.rules.datatype.gen.bean.writers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {
    /**
     * Key word for the default value.
     * Some kind of the default value should be used when this word is found
     */
    String DEFAULT = "_DEFAULT_";

    String value() default DEFAULT;
}
