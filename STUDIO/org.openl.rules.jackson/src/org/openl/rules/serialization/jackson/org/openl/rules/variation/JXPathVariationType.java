package org.openl.rules.serialization.jackson.org.openl.rules.variation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Custom mapping for {@link JXPathVariationType} due to it is not usual bean and should be initialized through
 * non-default constructor.
 *
 * @author Marat Kamalov
 */
public abstract class JXPathVariationType {
    public JXPathVariationType(@JsonProperty("variationID") String variationID,
            @JsonProperty("updatedArgumentIndex") int updatedArgumentIndex,
            @JsonProperty("path") String path,
            @JsonProperty("valueToSet") Object valueToSet) {
    }

}