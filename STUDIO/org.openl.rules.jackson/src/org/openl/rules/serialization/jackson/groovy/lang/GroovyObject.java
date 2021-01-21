package org.openl.rules.serialization.jackson.groovy.lang;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2021 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface GroovyObject {
    @JsonIgnore
    groovy.lang.MetaClass getMetaClass();

    @JsonIgnore
    void setMetaClass(groovy.lang.MetaClass metaClass);
}
