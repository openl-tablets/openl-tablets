package org.openl.rules.serialization.jackson.org.openl.rules.helpers;

/*
 * #%L
 * OpenL - Rules - Serialization
 * %%
 * Copyright (C) 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import org.openl.domain.IType;
import org.openl.rules.helpers.IntRange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Custom mapping for {@link IntRange} due to it is not usual bean all results
 * should be registered using the special methods.
 * 
 * @author Marat Kamalov
 */
public class IntRangeType {
    @JsonCreator
    public IntRangeType(@JsonProperty("min") int min, @JsonProperty("max") int max) {
    }

    @JsonIgnore
    public IType getElementType() {
        return null;
    }
    
    @JsonIgnore
    public boolean isFinite(){
        return false;
    }
}
