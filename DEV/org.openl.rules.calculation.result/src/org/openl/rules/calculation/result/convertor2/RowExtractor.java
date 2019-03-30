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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
            this.columnExtractors = new CopyOnWriteArrayList<>();
        } else {
            this.columnExtractors = new CopyOnWriteArrayList<>(columnExtractors);
        }
    }

    private NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> conf;

    public NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> getConf() {
        return conf;
    }

    public void setConf(NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> conf) {
        this.conf = conf;
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
     * Extract the given row from the given spreadsheet result and populates the row instance see
     * {@link #makeRowInstance()}
     * 
     * @param spreadsheetResult from which the row will be extracted
     * @param rowIndex index of the row for extraction
     * 
     * @return populated row instance with data from spreadsheet row.
     */
    public final T extract(SpreadsheetResult spreadsheetResult, int rowIndex) {
        T rowInstance = makeRowInstance();
        ConvertationMetadata convertationMetadata = null;
        if (getConf() != null && getConf().isConvertationMetadataEnabled()) {
            convertationMetadata = new ConvertationMetadata();
            convertationMetadata.setSpreadsheetResult(spreadsheetResult);
        }

        for (SpreadsheetColumnExtractor<T> extractor : columnExtractors) {
            String columnName = extractor.getColumn().getColumnName();
            int columnIndex = SpreadsheetResultHelper.getColumnIndexByName(columnName,
                spreadsheetResult.getColumnNames());
            Object columnValue = spreadsheetResult.getValue(rowIndex, columnIndex);
            Object v = extractor.convertAndStoreData(columnValue, rowInstance);
            if (convertationMetadata != null && v != null) {
                if (v instanceof ConvertationMetadata.NestedType) {
                    ConvertationMetadata.NestedType nestedType = (ConvertationMetadata.NestedType) v;
                    convertationMetadata.setNestedMetadata(nestedType, rowIndex, columnIndex);
                } else {
                    if (v instanceof String) {
                        convertationMetadata.addPropertyMetadata((String) v, rowIndex, columnIndex);
                    }
                }
            }
        }

        rowInstance.setStepName(spreadsheetResult.getRowName(rowIndex));

        // additional processing for the extracted row
        //
        rowInstance = afterExtract(rowInstance);

        if (convertationMetadata != null) {
            rowInstance.setConvertationMetadata(convertationMetadata);
        }

        return rowInstance;
    }
}
