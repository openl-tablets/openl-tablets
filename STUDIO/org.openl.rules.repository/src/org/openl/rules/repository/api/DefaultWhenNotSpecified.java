package org.openl.rules.repository.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Denotes that if this parameter is not specified for given repository, default value is always used. The default value is default per factory.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultWhenNotSpecified {
}
