package org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.variation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

/**
 * Custom mapping for {@link VariationsResultType}.
 * 
 * @author Marat Kamalov
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class VariationsResultType<T> {

    @JsonIgnore
    public String[] getCalculatedVariationIDs() {
        return null;
    }

    @JsonIgnore
    public String[] getFailedVariationIDs() {
        return null;
    }
    
    @JsonIgnore
    public String[] getAllProcessedVariationIDs() {
        return null;
    }
}
