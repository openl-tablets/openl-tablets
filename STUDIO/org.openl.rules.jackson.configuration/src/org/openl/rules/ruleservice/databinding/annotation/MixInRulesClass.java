package org.openl.rules.ruleservice.databinding.annotation;

/*-
 * #%L
 * OpenL - STUDIO - Jackson - Configuration
 * %%
 * Copyright (C) 2015 - 2020 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MixInRulesClass {
    String[] value();
}
