package org.openl.rules.calculation.result.convertor;

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
 * Factory for Spreadsheet row extractors. There are 2 type of rows:<br>
 * 1) simple row, it means no SpreadsheetResult or SpreadsheetResult[] values in
 * its columns<br>
 * 2) compound row, it means there is a nesting SpreadsheetResult or
 * SpreadsheetResult[] value in one of the columns
 * 
 * @author DLiauchuk
 * 
 */
public class NestedDataRowExtractorsFactory<T extends CodeStep, Q extends CompoundStep> {

    /** row extractor for simple rows */
    private RowExtractor<T> simpleRowExtractor;

    /** row extractor for compound rows */
    private RowExtractor<Q> compoundRowExtractor;

    public NestedDataRowExtractorsFactory(RowExtractor<T> simpleRowExtractor, RowExtractor<Q> compoundRowExtractor) {
        this.simpleRowExtractor = simpleRowExtractor;
        this.compoundRowExtractor = compoundRowExtractor;
    }

    /**
     * Gets the row extractor.
     * 
     * @param containsNested flag if the row contains any nested result
     * @return row extractor
     */
    public RowExtractor<?> getRowExtractor(boolean containsNested) {
        if (containsNested) {
            return compoundRowExtractor;
        }
        return simpleRowExtractor;
    }
}
