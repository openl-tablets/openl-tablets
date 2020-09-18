package org.openl.rules.serialization.jackson.org.openl.rules.variation;

/*
 * #%L
 * OpenL - Rules - Serialization
 * %%
 * Copyright (C) 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import org.openl.rules.variation.Variation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Custom mapping for {@link ComplexVariationType} due to it is not usual bean and should be initialized through
 * non-default constructor.
 *
 * @author Marat Kamalov
 */
public class ComplexVariationType {
    public ComplexVariationType(@JsonProperty("variationID") String variationID,
            @JsonProperty("variations") Variation... variations) {
    }
}
