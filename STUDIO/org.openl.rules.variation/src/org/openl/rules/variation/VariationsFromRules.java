package org.openl.rules.variation;

/*
 * #%L
 * OpenL - Variation
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify rules defining variations.
 * 
 * During the compilation method with name as specified in <code>ruleName</code>
 * and parameter types as in method marked by this annotation except of
 * {@link VariationsPack}
 * 
 * @author PUdalau
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated
public @interface VariationsFromRules {
    /**
     * Name of rule that defines vairations.
     */
    String ruleName();
}
