package org.openl.rules.calc.result.convertor;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */



/**
 * Class that holds the information about the column that need to be extracted from spreadsheet table.
 * 
 * @author DLiauchuk
 *
 */
@Deprecated
public class ColumnToExtract {
    
    private String columnName;
    private Class<?> expectedType;
    private boolean containNested;
    
    /**
     * 
     * @param columnName name of the column as it is in Spreadsheet table
     * @param expectedType type of the value to store extracted value
     * @param containNested indicates if there is any row for this column with nested result.
     * For information which values considered to be nested see {@link NestedDataRowExtractorsFactory} 
     */
    public ColumnToExtract(String columnName, Class<?> expectedType, boolean containNested) {
        this.columnName = columnName.split(":")[0].trim(); // Get the first part in case ColName:ColType is used
        this.expectedType = expectedType;
        this.containNested = containNested;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Class<?> getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(Class<?> expectedType) {
        this.expectedType = expectedType;
    }

    public boolean containNested() {
        return containNested;
    }

    public void setContainNested(boolean containNested) {
        this.containNested = containNested;
    }
}
