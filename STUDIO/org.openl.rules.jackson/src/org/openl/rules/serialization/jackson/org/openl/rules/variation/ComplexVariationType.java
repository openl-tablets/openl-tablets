package org.openl.rules.serialization.jackson.org.openl.rules.variation;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.openl.rules.variation.Variation;

/**
 * Custom mapping for {@link ComplexVariationType} due to it is not usual bean and should be initialized through
 * non-default constructor.
 *
 * @author Marat Kamalov
 */
public abstract class ComplexVariationType {
    public ComplexVariationType(@JsonProperty("variationID") String variationID,
                                @JsonProperty("variations") Variation... variations) {
    }
}
