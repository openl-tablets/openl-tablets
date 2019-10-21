package org.openl.rules.calculation.result.convertor2;

import org.openl.rules.calc.SpreadsheetResult;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

/**
 * Class that holds the information about the column that need to be extracted from spreadsheet table.
 *
 * @author Marat Kamalov
 *
 */
public class ColumnToExtract {

    private String columnName;

    private String[] propertyNames;

    private Class<?>[] propertyTypes;

    private int nestedPriority = Integer.MAX_VALUE - 1;

    /**
     * Creates a ColumnToExtract with column name. Requred for nested columns.
     *
     * @param columnName column name
     */
    public ColumnToExtract(String columnName) {
        this(columnName, columnName, SpreadsheetResult.class);
    }

    public ColumnToExtract(String columnName, Class<?> expectedType, int nestedPriority) {
        this(columnName, expectedType);
        this.nestedPriority = nestedPriority;
    }

    /**
     * Creates a ColumnToExtract with column name and expected type.
     *
     * @param columnName column name
     * @param expectedType expected type
     */
    public ColumnToExtract(String columnName, Class<?> expectedType) {
        this(columnName, columnName, expectedType);
    }

    public ColumnToExtract(String columnName, String propertyName, Class<?> expectedType, int nestedPriority) {
        this(columnName, propertyName, expectedType);
        this.nestedPriority = nestedPriority;
    }

    /**
     * Creates a ColumnToExtract with column name, property name and expected type. Property name is used for storing
     * value into row instance.
     *
     * @param columnName column name
     * @param propertyName property name
     * @param expectedType expected type
     */
    public ColumnToExtract(String columnName, String propertyName, Class<?> expectedType) {
        this(columnName, new String[] { propertyName }, new Class<?>[] { expectedType });
    }

    public ColumnToExtract(String columnName, String[] propertyNames, Class<?>[] expectedTypes, int nestedPriority) {
        this(columnName, propertyNames, expectedTypes);
        this.nestedPriority = nestedPriority;
    }

    /**
     * Creates a ColumnToExtract with column name, property names and expected types. Property names are used for
     * storing value into row instance. If value cannot be stored in row instance next property name and expected type is
     * used.
     *
     * @param columnName column name
     * @param propertyName property name
     * @param expectedType expected type
     */
    public ColumnToExtract(String columnName, String[] propertyNames, Class<?>[] expectedTypes) {
        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("columnName cannot be null or empty!");
        }
        if (propertyNames == null || propertyNames.length == 0) {
            throw new IllegalArgumentException("propertyName cannot be null or empty!");
        }
        if (expectedTypes == null || expectedTypes.length == 0) {
            throw new IllegalArgumentException("expectedTypes cannot be null or empty!");
        }
        if (expectedTypes.length != propertyNames.length) {
            throw new IllegalArgumentException("expectedTypes and propertyNames should be the same length!");
        }
        this.columnName = columnName.split(":")[0].trim(); // Get the first part in case ColName:ColType is used
        int i = 0;
        for (String propertyName : propertyNames) {
            if (propertyName == null) {
                throw new IllegalArgumentException("propertyName cannot be null or empty.");
            }
            if (expectedTypes[i] == null) {
                throw new IllegalArgumentException("expectedType cannot be null");
            }
        }
        this.propertyNames = propertyNames;
        this.propertyTypes = expectedTypes;
    }

    /**
     * Return column name.
     *
     * @return column name.
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Return expected type for property name.
     *
     * @return expected type.
     */
    public Class<?>[] getExpectedTypes() {
        return propertyTypes;
    }

    /**
     * Returns property names for this column defenition.
     *
     * @return expected type.
     */
    public String[] getPropertyNames() {
        return propertyNames;
    }

    public int getNestedPriority() {
        return nestedPriority;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ColumnToExtract other = (ColumnToExtract) obj;
        if (columnName == null) {
            if (other.columnName != null) {
                return false;
            }
        } else if (!columnName.equals(other.columnName)) {
            return false;
        }
        return true;
    }

}
