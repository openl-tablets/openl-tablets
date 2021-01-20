package org.openl.rules.serialization.jackson.org.openl.rules.variation;

import org.openl.rules.variation.Variation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Custom mapping for {@link DeepCloningVariationType} due to it is not usual bean and should be initialized through
 * non-default constructor.
 *
 * @author Marat Kamalov
 */
public abstract class DeepCloningVariationType {
    public DeepCloningVariationType(@JsonProperty("variationID") String variationID,
            @JsonProperty("variation") Variation variation) {
    }

    @JsonIgnore
    public Variation getDelegatedVariation() {
        return null;
    }
}
