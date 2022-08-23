package org.openl.rules.ruleservice.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If service method parameter is marked with @{@link BeanToSpreadsheetResultConvert} then bean to spreadsheet result
 * conversation is required before passing parameter to rules method. Can be used on @{@link java.util.Collection} types
 * also.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface BeanToSpreadsheetResultConvert {
}
