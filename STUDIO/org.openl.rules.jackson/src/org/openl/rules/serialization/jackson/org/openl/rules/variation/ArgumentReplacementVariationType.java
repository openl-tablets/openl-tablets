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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Custom mapping for {@link ArgumentReplacementVariationType} due to it is not usual bean and should be initialized
 * through non-default constructor.
 *
 * @author Marat Kamalov
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class ArgumentReplacementVariationType {
    public ArgumentReplacementVariationType(@JsonProperty("variationID") String variationID,
            @JsonProperty("updatedArgumentIndex") int updatedArgumentIndex,
            @JsonProperty("valueToSet") Object valueToSet) {
    }
}
