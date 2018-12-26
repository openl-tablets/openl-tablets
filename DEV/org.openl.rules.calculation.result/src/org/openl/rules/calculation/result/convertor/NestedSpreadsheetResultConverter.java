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


import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.SpreadsheetResultHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
@Deprecated
public class NestedSpreadsheetResultConverter<T extends CodeStep, Q extends CompoundStep> {

    private final Logger log = LoggerFactory.getLogger(NestedSpreadsheetResultConverter.class);

    private NestedDataRowExtractorsFactory<T, Q> rowExtractorsFactory;

    private NestedSpreadsheetConfiguration<T, Q> conf;

    private int currentNestingLevel;

    /**
     * @param currentNestingLevel the number of the current nesting level
     * @param configuration       configuration that is used for extracting rows on
     *                            this and further levels, connat be null. In that case will
     *                            throw {@link IllegalArgumentException}
     */
    public NestedSpreadsheetResultConverter(int currentNestingLevel, NestedSpreadsheetConfiguration<T, Q> configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be empty");
        }
        this.conf = configuration;
        this.currentNestingLevel = currentNestingLevel;
        this.rowExtractorsFactory = configuration.getRowExtractorsFactory(currentNestingLevel);
    }

    /**
     * Converts the spreadsheet result to flat structure.
     *
     * @param spreadsheetResult {@link SpreadsheetResult} that is going to be
     *                          converted.
     * @return converted result, represented in flat structure.
     */
    public List<CodeStep> process(SpreadsheetResult spreadsheetResult) {
        List<CodeStep> steps = new ArrayList<CodeStep>();
        if (spreadsheetResult != null) {
            int height = spreadsheetResult.getHeight();

            for (int row = 0; row < height; row++) {
                CodeStep step = processRow(spreadsheetResult, row);
                steps.add(step);
            }
            return steps;
        }
        log.warn("Spreadsheet result is null");
        return steps;
    }

    private CodeStep processRow(SpreadsheetResult spreadsheetResult, int row) {

        CodeStep step = null;

        RowExtractor<?> rowExtractor = rowExtractorsFactory
                .getRowExtractor(anyNestedValueInRow(spreadsheetResult, row));
        step = rowExtractor.extract(spreadsheetResult, row);

        step.setStepName(spreadsheetResult.getRowName(row));
        return step;
    }

    private boolean anyNestedValueInRow(SpreadsheetResult spreadsheetResult, int row) {
        List<ColumnToExtract> compoundColumns = conf.getCompoundColumnsToExtract(currentNestingLevel);

        for (ColumnToExtract column : compoundColumns) {
            int columnIndex = SpreadsheetResultHelper.getColumnIndexByName(column.getColumnName(),
                    spreadsheetResult.getColumnTitles());
            Object valueInColumn = spreadsheetResult.getValue(row, columnIndex);
            if (containsNested(valueInColumn)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNested(Object value) {
        // TODO: fix me
        if ((value instanceof SpreadsheetResult) || (value instanceof SpreadsheetResult[])) {
            return true;
        }
        return false;
    }

}
