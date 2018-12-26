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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The example of flat spreadsheet result structure.
 *                      |---------SimpleRow                     |---------SimpleRow   
 *                      |                                       |   
 *                      |---------CompoundSecondLevelResult-----|---------SimpleRow
 *                      |                                       |
 * UpperLevelResult-----|---------SimpleRow                     |---------CompoundThirdLevelResult1----.....
 *                      |                                       |            
 *                      |---------SimpleRow                     |---------CompoundThirdLevelResult2----.....
 *                      |                                       |    
 *                      |---------SimpleRow                     |---------SimpleRow    
 */

/**
 * SpreadsheetResult convertor that supports nested SpreadsheetResult as column
 * values. Converts the SpreadsheetResult to flat structure.
 *
 * @param <T> class that will be populated with values, when extracting rows
 *            without compound results.
 * @param <Q> class that will be populated with values, when extracting rows wit
 *            compound results.
 * @author DLiauchuk
 */
public class NestedSpreadsheetResultConverter<T extends CalculationStep, Q extends CompoundStep> {

    private final Logger log = LoggerFactory.getLogger(NestedSpreadsheetResultConverter.class);

    private NestedSpreadsheetConfiguration<T, Q> conf;
    
    private int currentNestingLevel;

    /**
     * @param currentNestingLevel the number of the current nesting level
     * @param configuration configuration that is used for extracting rows on
     *            this and further levels, connat be null. In that case will
     *            throw {@link IllegalArgumentException}
     */
    public NestedSpreadsheetResultConverter(int currentNestingLevel, NestedSpreadsheetConfiguration<T, Q> configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be empty");
        }
        this.conf = configuration;
        this.currentNestingLevel = currentNestingLevel;
    }

    /**
     * Converts the spreadsheet result to flat structure.
     *
     * @param spreadsheetResult {@link SpreadsheetResult} that is going to be
     *            converted.
     * @return converted result, represented in flat structure.
     */
    public List<CalculationStep> process(SpreadsheetResult spreadsheetResult) {
        List<CalculationStep> steps = new ArrayList<CalculationStep>();
        if (spreadsheetResult != null) {
            int height = spreadsheetResult.getHeight();
            RowFilter rowFilter = conf.buildRowFilter();
            for (int row = 0; row < height; row++) {
                if (rowFilter == null || !rowFilter.excludeRow(spreadsheetResult.getRowName(row))) {
                    CalculationStep step = processRow(spreadsheetResult, row);
                    if (step != null){
                        steps.add(step);
                    }
                }
            }
            return steps;
        }
        if (log.isWarnEnabled()){
            log.warn("Spreadsheet result is null");
        }
        return steps;
    }

    @SuppressWarnings("unchecked")
    private CalculationStep processRow(SpreadsheetResult spreadsheetResult, int row) {
        T step = null;
        List<ColumnToExtract> confCompoundColumns = conf.getColumnsToExtract(currentNestingLevel);
        List<ColumnToExtract> compoundColumns = new ArrayList<ColumnToExtract>();
        List<SpreadsheetColumnExtractor<Q>> extractors = new ArrayList<SpreadsheetColumnExtractor<Q>>();
        
        //Find existed columns in spreadsheetResult
        for (ColumnToExtract column : confCompoundColumns){
            int columnIndex = SpreadsheetResultHelper.getColumnIndex(column.getColumnName(),
                spreadsheetResult.getColumnNames());
            if (columnIndex >= 0){
                compoundColumns.add(column);
            }else{
                if (log.isDebugEnabled()){
                    log.debug("Column {} was skipped!", column.getColumnName());
                }
            }
        }
        
        boolean isNestedRow = false;
        int minNestedPriority = -1;
        for (ColumnToExtract column : compoundColumns) {
            int columnIndex = SpreadsheetResultHelper.getColumnIndexByName(column.getColumnName(),
                spreadsheetResult.getColumnNames());
            Object valueInColumn = spreadsheetResult.getValue(row, columnIndex);
            if (valueIsNested(valueInColumn)) {
                if (column.getNestedPriority() > 0 && (column.getNestedPriority() <= minNestedPriority || minNestedPriority == -1)){
                    minNestedPriority = column.getNestedPriority(); 
                }
            }
        }
        
        for (ColumnToExtract column : compoundColumns) {
            int columnIndex = SpreadsheetResultHelper.getColumnIndexByName(column.getColumnName(),
                spreadsheetResult.getColumnNames());
            Object valueInColumn = spreadsheetResult.getValue(row, columnIndex);
            if (!isNestedRow && minNestedPriority > 0 && minNestedPriority == column.getNestedPriority() && valueIsNested(valueInColumn)) {
                extractors.add((SpreadsheetColumnExtractor<Q>) conf.initCompoundColumnExtractor(currentNestingLevel,
                    column));
                isNestedRow = true;
            } else {
                extractors.add(new SpreadsheetColumnExtractor<Q>(column, conf));
            }
        }
        RowExtractor<? extends CalculationStep> rowExtractor = null;
        if (isNestedRow) {
            rowExtractor = conf.initCompoundRowExtractor(extractors);
            rowExtractor.setConf(conf);
        } else {
            List<SpreadsheetColumnExtractor<T>> simpleExtractors = new ArrayList<SpreadsheetColumnExtractor<T>>();
            for (ColumnToExtract column : compoundColumns) {
                simpleExtractors.add(new SpreadsheetColumnExtractor<T>(column, conf));
            }
            rowExtractor = conf.initSimpleRowExtractor(simpleExtractors);
            rowExtractor.setConf(conf);
        }

        step = (T) rowExtractor.extract(spreadsheetResult, row);
        return step;
    }

    private static boolean valueIsNested(Object value) {
        if ((value instanceof SpreadsheetResult) || (value instanceof SpreadsheetResult[])) {
            return true;
        }
        return false;
    }

}
