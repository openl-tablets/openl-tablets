package org.openl.rules.calc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD, ElementType.METHOD })
public @interface SpreadsheetCell {
    String column();

    String row();

    boolean simpleRefByRow() default false;

    boolean simpleRefByColumn() default false;
}
