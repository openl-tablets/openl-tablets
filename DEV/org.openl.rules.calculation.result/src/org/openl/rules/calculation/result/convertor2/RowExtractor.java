package org.openl.rules.calculation.result.convertor2;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.SpreadsheetResultHelper;

/**
 * Extractor for the appropriate row in spreadsheet.
 * 
 * @author DLiauchuk
 * 
 * @param <T>
 */
public abstract class RowExtractor<T extends CalculationStep> {

    /** extractors for columns */
    private List<SpreadsheetColumnExtractor<T>> columnExtractors;

    public RowExtractor(List<SpreadsheetColumnExtractor<T>> columnExtractors) {
        if (columnExtractors == null) {
            this.columnExtractors = new ArrayList<SpreadsheetColumnExtractor<T>>();
        } else {
            this.columnExtractors = new ArrayList<SpreadsheetColumnExtractor<T>>(columnExtractors);
        }
    }

    /**
     * Creates the instance describing the row.
     * 
     * @return <T> the row instance
     */
    protected abstract T makeRowInstance();

    /**
     * Additional processing for the extracted row. Do not implement by default.
     * 
     * @param step
     */
    protected abstract T afterExtract(T step);

    /**
     * Extract the given row from the given spreadsheet result and populates the
     * row instance see {@link #makeRowInstance()}
     * 
     * @param spreadsheetResult from which the row will be extracted
     * @param rowIndex index of the row for extraction
     * 
     * @return populated row instance with data from spreadsheet row.
     */
    public T extract(SpreadsheetResult spreadsheetResult, int rowIndex) {
        T rowInstance = makeRowInstance();
        for (SpreadsheetColumnExtractor<T> extractor : columnExtractors) {
            String columnName = extractor.getColumn().getColumnName();
            int columnIndex = SpreadsheetResultHelper.getColumnIndexByName(columnName,
                spreadsheetResult.getColumnNames());
            Object columnValue = spreadsheetResult.getValue(rowIndex, columnIndex);
            extractor.convertAndStoreData(columnValue, rowInstance);
        }
        
        rowInstance.setStepName(spreadsheetResult.getRowName(rowIndex));
        
        // additional processing for the extracted row
        //
        rowInstance = afterExtract(rowInstance);
        return rowInstance;
    }
}
