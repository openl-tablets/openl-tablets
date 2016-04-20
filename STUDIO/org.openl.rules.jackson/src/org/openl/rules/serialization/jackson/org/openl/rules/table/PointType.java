package org.openl.rules.serialization.jackson.org.openl.rules.table;

/*
 * #%L
 * OpenL - Rules - Serialization
 * %%
 * Copyright (C) 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Custom mapping for {@link PointType} due to it is not usual bean all results
 * should be registered using the special methods.
 * 
 * @author Marat Kamalov
 */
public class PointType {
    @JsonCreator
    public PointType(@JsonProperty("column") int column, @JsonProperty("row") int row) {
    }
}
