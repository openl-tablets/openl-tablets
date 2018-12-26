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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openl.meta.DoubleValue;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.SpreadsheetResultHelper;

/**
 * Extractor for the appropriate row in spreadsheet.
 * 
 * @author DLiauchuk
 *
 * @param <T>
 */
@Deprecated
public abstract class RowExtractor<T extends CodeStep> {
    
    /** extractors for columns*/
    private List<SpreadsheetColumnExtractor<T>> columnExtractors;
    
    public RowExtractor(List<SpreadsheetColumnExtractor<T>> columnExtractors) {        
        this.columnExtractors = new CopyOnWriteArrayList<SpreadsheetColumnExtractor<T>>(columnExtractors);
    }
    
    /**
     * Creates the instance describing the row.
     * 
     * @return <T> the row instance
     */
    protected abstract T makeRowInstance();
    
    /**
     * Additional processing for the extracted row.
     * Do not implement by default.
     * 
     * @param step
     */
    protected abstract void afterExtract(T step);
    
    /**
     * Extract the given row from the given spreadsheet result and populates the row 
     * instance see {@link #makeRowInstance()}
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
            int columnIndex = SpreadsheetResultHelper.getColumnIndexByName(columnName, spreadsheetResult.getColumnNames());
            Object columnValue = spreadsheetResult.getValue(rowIndex, columnIndex);            
            if (isSuitableValue(columnValue)) {
                extractor.convertAndStoreData(columnValue, rowInstance);
            }
        }
        
        // additional processing for the extracted row
        //
        afterExtract(rowInstance);
        return rowInstance;
    }
    
    // TODO: delete
    private boolean isSuitableValue(Object columnValue) {
        if (columnValue != null) {
            if (columnValue instanceof String || columnValue instanceof StringValue || columnValue instanceof DoubleValue || 
                    columnValue instanceof SpreadsheetResult || columnValue instanceof SpreadsheetResult[]) {
                return true;
            }   
        }
        return false;
    }

    
}
