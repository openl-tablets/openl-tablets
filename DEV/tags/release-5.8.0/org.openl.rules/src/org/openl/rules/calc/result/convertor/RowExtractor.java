package org.openl.rules.calc.result.convertor;

import java.util.ArrayList;
import java.util.List;

import org.openl.meta.DoubleValue;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultUtils;

/**
 * Extractor for the appropriate row in spreadsheet.
 * 
 * @author DLiauchuk
 *
 * @param <T>
 */
public abstract class RowExtractor<T extends CodeStep> {
    
    /** extractors for columns*/
    private List<SpreadsheetColumnExtractor<T>> columnExtractors;
    
    public RowExtractor(List<SpreadsheetColumnExtractor<T>> columnExtractors) {        
        this.columnExtractors = new ArrayList<SpreadsheetColumnExtractor<T>>(columnExtractors);
    }
    
    protected abstract T makeRowInstance();
    
    public T extract(SpreadsheetResult spreadsheetResult, int rowIndex) {
        T rowInstance = makeRowInstance();
        for (SpreadsheetColumnExtractor<T> extractor : columnExtractors) {
            String columnName = extractor.getColumn().getColumnName();
            int columnIndex = SpreadsheetResultUtils.getColumnIndexByName(columnName, spreadsheetResult.getColumnNames());
            Object columnValue = spreadsheetResult.getValue(rowIndex, columnIndex);            
            if (isSuitableValue(columnValue)) {
                extractor.convertAndStoreData(columnValue, rowInstance);
            }
        }
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
