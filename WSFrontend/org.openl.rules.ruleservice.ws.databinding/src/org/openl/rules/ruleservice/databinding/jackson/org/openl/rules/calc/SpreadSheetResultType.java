package org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.calc;

import java.util.Map;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
 * Custom mapping for {@link SpreadSheetResult} due to it is not usual bean all
 * results should be registered using the special methods.
 * 
 * @author Marat Kamalov
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class SpreadSheetResultType {

    @JsonCreator
    public SpreadSheetResultType(@JsonProperty("results") Object[][] results,
            @JsonProperty("rowNames") String[] rowNames,
            @JsonProperty("columnNames") String[] columnNames,
            @JsonProperty("fieldsCoordinates") Map<String, Point> fieldsCoordinates) {
    }

    @JsonIgnore
    public int getHeight() {
        return 0;
    }

    @JsonIgnore
    public int getWidth() {
        return 0;
    }
    
    @JsonIgnore
    public ILogicalTable getLogicalTable() {
        return null;
    }

}
