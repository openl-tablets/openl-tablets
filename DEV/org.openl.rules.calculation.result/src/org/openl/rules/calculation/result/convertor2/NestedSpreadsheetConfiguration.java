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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.convertor.ObjectToDataOpenCastConvertor;

/**
 * Configuration for the nested spreadsheet result converter. Extend it by overriding abstract methods
 * {@link #initCompoundRowExtractor(List)} and {@link #initSimpleRowExtractor(List)}
 *
 * @author DLiauchuk
 *
 * @param <T> class that will be populated with values, when extracting rows without compound results.
 * @param <Q> class that will be populated with values, when extracting rows wit compound results.
 */
public abstract class NestedSpreadsheetConfiguration<T extends CalculationStep, Q extends CompoundStep> {

    /** Map of columns that gonna be extracted on each level of extracting **/
    private Map<Integer, List<ColumnToExtract>> columnsToExtractForLevels;

    /** Row filter **/
    private RowFilter rowFilter;

    private ObjectToDataOpenCastConvertor objectToDataOpenCastConvertor = new ObjectToDataOpenCastConvertor();

    public ObjectToDataOpenCastConvertor getObjectToDataOpenCastConvertor() {
        return objectToDataOpenCastConvertor;
    }

    /**
     *
     * @param columnsToExtractForLevels Map of columns that gonna be extracted on each level of extracting key: number
     *            of the nesting level. value: list of columns to extract
     */
    public NestedSpreadsheetConfiguration(Map<Integer, List<ColumnToExtract>> columnsToExtractForLevels) {
        this.columnsToExtractForLevels = new HashMap<>(columnsToExtractForLevels);
    }

    /**
     *
     * @param nestingLevel current nesting level
     * @return list of columns that must be extracted on given level
     */
    public List<ColumnToExtract> getColumnsToExtract(int nestingLevel) {
        return new ArrayList<>(columnsToExtractForLevels.get(nestingLevel));
    }

    /**
     * Implement this method to return {@link RowExtractor} for the rows with only simple columns.
     *
     * @param simpleExtractors extractors for simple columns
     * @return {@link RowExtractor} for the rows with only simple columns
     */
    protected abstract RowExtractor<T> initSimpleRowExtractor(List<SpreadsheetColumnExtractor<T>> simpleExtractors);

    /**
     * Implement this method to return {@link RowExtractor} for the rows contain compound columns.
     *
     * @param compoundExtractors extractors for compound columns
     * @return {@link RowExtractor} for the rows contain compound columns
     */
    protected abstract RowExtractor<Q> initCompoundRowExtractor(List<SpreadsheetColumnExtractor<Q>> compoundExtractors);

    protected boolean isConvertationMetadataEnabled() {
        return false;
    }

    /**
     * Initialize the extractor for the column that is compound.
     *
     * @param nestingLevel current level of nesting
     * @param columnToExtract column for extraction
     * @return the extractor for the column that is compound.
     */
    protected NestedSpreadsheedColumnExtractor initCompoundColumnExtractor(int nestingLevel,
            ColumnToExtract columnToExtract) {
        return new NestedSpreadsheedColumnExtractor(nestingLevel, this, columnToExtract);
    }

    /**
     * Initialize rowFilter
     */
    protected RowFilter initRowFilter() {
        return null;
    }

    /**
     * Initialize rowFilter
     */
    public final RowFilter buildRowFilter() {
        if (rowFilter == null) {
            rowFilter = initRowFilter();
        }
        return rowFilter;
    }

}
