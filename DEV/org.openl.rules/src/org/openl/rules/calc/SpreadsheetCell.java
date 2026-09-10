package org.openl.rules.calc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface SpreadsheetCell {
    String column() default "";

    String row() default "";

    String cell();

    /**
     * Returns the suffix that keeps duplicate generated spreadsheet fields distinct.
     *
     * <p>
     * The suffix is included in serialized property names after applying a spreadsheet result naming strategy.
     *
     * @return the generated field suffix, or an empty string when the field name is unique
     */
    String suffix() default "";
}
