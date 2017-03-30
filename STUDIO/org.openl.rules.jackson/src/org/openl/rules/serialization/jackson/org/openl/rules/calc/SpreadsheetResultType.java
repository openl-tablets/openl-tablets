package org.openl.rules.serialization.jackson.org.openl.rules.calc;

/*
 * #%L
 * OpenL - Rules - Serialization
 * %%
 * Copyright (C) 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.Map;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Custom mapping for {@link SpreadSheetResult} due to it is not usual bean all
 * results should be registered using the special methods.
 * 
 * @author Marat Kamalov
 */
public class SpreadsheetResultType {

    @JsonCreator
    public SpreadsheetResultType(@JsonProperty("results") Object[][] results,
            @JsonProperty("rowNames") String[] rowNames,
            @JsonProperty("columnNames") String[] columnNames,
            @JsonProperty("rowTitles") String[] rowTitles,
            @JsonProperty("columnTitles") String[] columnTitles,
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
