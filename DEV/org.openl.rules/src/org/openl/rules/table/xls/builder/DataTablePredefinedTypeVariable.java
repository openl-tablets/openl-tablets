package org.openl.rules.table.xls.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A class that describes a Data Table's table type variable for predefined types such as String, BigDecimal, IntRange
 * etc
 * 
 * @author NSamatov
 */
public final class DataTablePredefinedTypeVariable extends DataTableField {
    private List<DataTableField> availableFields;

    /**
     * Create a variable of a given type
     * 
     * @param typeName type of a variable
     */
    public DataTablePredefinedTypeVariable(String typeName) {
        super(typeName, typeName.toUpperCase());
        availableFields = Collections.<DataTableField> unmodifiableList(Arrays.asList(new ThisField()));
    }

    /**
     * Get a list containing one "this" field
     */
    @Override
    protected List<DataTableField> getAvailableFields() {
        return availableFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeName() {
        return getName();
    }

    /**
     * Always true
     */
    @Override
    public boolean isComplex() {
        return true;
    }

    /**
     * Fictional "this" field
     * 
     * @author NSamatov
     */
    private class ThisField extends DataTableField {
        private static final String PREDEFINED_TYPE_FIELD_NAME = "this";
        private static final String PREDEFINED_TYPE_FIELD_VALUE = "Value";

        private ThisField() {
            super(PREDEFINED_TYPE_FIELD_NAME, PREDEFINED_TYPE_FIELD_VALUE);
        }

        /**
         * Empty list
         */
        @Override
        protected List<DataTableField> getAvailableFields() {
            return Collections.emptyList();
        }

        /**
         * Always false
         */
        @Override
        public boolean isComplex() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getTypeName() {
            return DataTablePredefinedTypeVariable.this.getTypeName();
        }
    }
}
