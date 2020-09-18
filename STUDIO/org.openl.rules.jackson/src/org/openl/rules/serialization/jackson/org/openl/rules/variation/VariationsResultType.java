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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Custom mapping for {@link VariationsResultType}.
 *
 * @author Marat Kamalov
 */
public class VariationsResultType {

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
