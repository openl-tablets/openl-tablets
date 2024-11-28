package org.openl.rules.ruleservice.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used when custom API error model is defined in rules. This annotation must be added on service
 * class interface Usage:
 *
 * <pre>
 * {@code
 * import org.openl.generated.beans.MyError
 * import org.openl.generated.spreadsheetresults.MySPRError
 *
 * @ApiErrors(MyError.class, MySPRError.class)
 * interface Service {
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ApiErrors {

    Class<?>[] value();

}
