package org.openl.rules.serialization.jackson.org.openl.rules.variation;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Custom mapping for {@link VariationsResultType}.
 *
 * @author Marat Kamalov
 */
public abstract class VariationsResultType {

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
