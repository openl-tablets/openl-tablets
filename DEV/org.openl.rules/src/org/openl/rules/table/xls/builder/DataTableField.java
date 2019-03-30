package org.openl.rules.table.xls.builder;

import org.openl.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class, containing description of Data Table's field columns (or variables)
 * 
 * @author NSamatov
 */
public abstract class DataTableField {
    private String name;
    private String businessName;
    private String foreignKeyTable;
    private String foreignKeyColumn;

    private List<DataTableField> aggregatedFields = new ArrayList<>();

    /**
     * Create a field with a given technical name and business name
     * 
     * @param name technical name of a field
     * @param businessName business name of a field
     */
    public DataTableField(String name, String businessName) {
        this.name = name;
        this.businessName = businessName;
    }

    /**
     * Get a technical name of a field
     * 
     * @return technical name of a field
     */
    public String getName() {
        return name;
    }

    /**
     * Get a business name of a field
     * 
     * @return business name of a field
     */
    public String getBusinessName() {
        return businessName;
    }

    /**
     * Set business name of a field
     * 
     * @param businessName business name of a field
     */
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    /**
     * Get a foreign key table name for a field
     * 
     * @return foreign key table name for a field
     */
    public String getForeignKeyTable() {
        return foreignKeyTable;
    }

    /**
     * Set a foreign key table name for a field
     * 
     * @param foreignKeyTable foreign key table name for a field
     */
    public void setForeignKeyTable(String foreignKeyTable) {
        this.foreignKeyTable = foreignKeyTable;
    }

    /**
     * Get a foreign key column name for a field
     * 
     * @return foreign key column name for a field
     */
    public String getForeignKeyColumn() {
        return foreignKeyColumn;
    }

    /**
     * Set a foreign key column name for a field
     * 
     * @param foreignKeyColumn foreign key column name for a field
     */
    public void setForeignKeyColumn(String foreignKeyColumn) {
        this.foreignKeyColumn = foreignKeyColumn;
    }

    /**
     * Get a foreign key for a field
     * 
     * @return foreign key for a field
     */
    public String getForeignKey() {
        if (StringUtils.isBlank(foreignKeyTable))
            return null;

        String fk = ">" + foreignKeyTable;

        if (StringUtils.isNotBlank(foreignKeyColumn))
            fk += " " + foreignKeyColumn;

        return fk;
    }

    /**
     * Get a list of an aggregated fields that will be filled instead of referencing with a foreign key
     * 
     * @return list of an aggregated fields
     */
    public List<DataTableField> getAggregatedFields() {
        return aggregatedFields;
    }

    /**
     * Set a list of an aggregated fields that will be filled instead of referencing with a foreign key
     * 
     * @param aggregatedFields list of an aggregated fields
     */
    public void setAggregatedFields(List<DataTableField> aggregatedFields) {
        this.aggregatedFields = aggregatedFields;
    }

    /**
     * Returns a method of filling data for a field
     * 
     * @return if true then data is filled using aggregated fields else data is filled using foreign keys
     */
    public boolean isFillChildren() {
        return !getAggregatedFields().isEmpty();
    }

    /**
     * Use an aggregated fields instead of a foreign key
     * 
     * @see #useForeignKey()
     */
    public void useAggregatedFields() {
        setAggregatedFields(getAvailableFields());
    }

    /**
     * Use a foreign key instead of an aggregated fields
     * 
     * @see #useAggregatedFields()
     */
    public void useForeignKey() {
        setAggregatedFields(Collections.<DataTableField> emptyList());
    }

    /**
     * Get the field's type name
     * 
     * @return type name
     */
    public abstract String getTypeName();

    /**
     * Determine, if a field is a complex object
     * 
     * @return true, if a field is a complex
     */
    public abstract boolean isComplex();

    /**
     * Get available fields of a complex object
     * 
     * @return list of a child fields inside this complex object
     */
    protected abstract List<DataTableField> getAvailableFields();
}
