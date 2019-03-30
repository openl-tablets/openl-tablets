package org.openl.rules.calculation.result.convertor2;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openl.rules.calc.SpreadsheetResult;

public class ConvertationMetadata {
    private SpreadsheetResult spreadsheetResult;
    
    public SpreadsheetResult getSpreadsheetResult() {
        return spreadsheetResult;
    }
    
    public void setSpreadsheetResult(SpreadsheetResult spreadsheetResult) {
        this.spreadsheetResult = spreadsheetResult;
    }
    
    private Map<String, SpreadsheetResultPoint> data = new HashMap<>();
    
    public void addPropertyMetadata(String propertyName, int rowIndex, int columnIndex){
        data.put(propertyName, new SpreadsheetResultPoint(rowIndex, columnIndex));        
    }
    
    public Set<String> getProperties(){
        return Collections.unmodifiableSet(data.keySet());
    }
    
    public SpreadsheetResultPoint getPropert(String property){
        return data.get(property);
    }
    
    private NestedType nestedType;
    private int nestedRowIndex;
    private int nestedColumnIndex;
    
    public void setNestedMetadata(NestedType nestedType, int rowIndex, int columnIndex){
        this.nestedType = nestedType;
        this.nestedRowIndex = rowIndex;
        this.nestedColumnIndex = columnIndex;
    }
    
    public NestedType getNestedType() {
        return nestedType;
    }
    
    public int getNestedColumnIndex() {
        return nestedColumnIndex;
    }
    
    public int getNestedRowIndex() {
        return nestedRowIndex;
    }
    
    public enum NestedType{
        ARRAY, SINGLE
    }
}
