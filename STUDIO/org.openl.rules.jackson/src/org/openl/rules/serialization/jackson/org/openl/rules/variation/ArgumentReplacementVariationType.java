package org.openl.rules.serialization.jackson.org.openl.rules.variation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Custom mapping for {@link ArgumentReplacementVariationType} due to it is not usual bean and should be initialized
 * through non-default constructor.
 *
 * @author Marat Kamalov
 */
public abstract class ArgumentReplacementVariationType {
    public ArgumentReplacementVariationType(@JsonProperty("variationID") String variationID,
            @JsonProperty("updatedArgumentIndex") int updatedArgumentIndex,
            @JsonProperty("valueToSet") Object valueToSet) {
    }
}
