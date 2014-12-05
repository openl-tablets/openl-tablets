package org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.table;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
