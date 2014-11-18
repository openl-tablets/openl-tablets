package org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.variation;

import org.openl.rules.variation.Variation;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Custom mapping for {@link ComplexVariationType} due to it is not
 * usual bean and should be initialized through non-default constructor.
 * 
 * @author Marat Kamalov
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class ComplexVariationType {
    public ComplexVariationType(@JsonProperty("variationID") String variationID,
            @JsonProperty("variations") Variation... variations) {
    }
}
