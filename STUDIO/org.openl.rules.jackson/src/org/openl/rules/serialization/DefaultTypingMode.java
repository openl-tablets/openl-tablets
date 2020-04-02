package org.openl.rules.serialization;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2018 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */
public enum DefaultTypingMode {
    JAVA_LANG_OBJECT,
    OBJECT_AND_NON_CONCRETE,
    NON_CONCRETE_AND_ARRAYS,
    NON_FINAL,
    EVERYTHING,
    DISABLED;
}
