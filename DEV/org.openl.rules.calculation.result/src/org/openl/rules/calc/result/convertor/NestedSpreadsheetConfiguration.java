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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the nested spreadsheet result converter.
 * Extend it by overriding abstract methods {@link #initCompoundRowExtractor(List)} and {@link #initSimpleRowExtractor(List)}
 * 
 * @author DLiauchuk
 *
 * @param <Simple> class that will be populated with values, when extracting rows without compound results.
 * @param <Compound> class that will be populated with values, when extracting rows wit compound results.
 */
@Deprecated
public abstract class NestedSpreadsheetConfiguration<Simple extends CodeStep, Compound extends CompoundStep> {
    
    /** Map of columns that gonna be extracted on each level of extracting**/
    private Map<Integer, List<ColumnToExtract>> columnsToExtractForLevels;
    
    /**
     * 
     * @param columnsToExtractForLevels Map of columns that gonna be extracted on each level of extracting
     * key: number of the nesting level. value: list of columns to extract
     */
    public NestedSpreadsheetConfiguration(Map<Integer, List<ColumnToExtract>> columnsToExtractForLevels) {
        this.columnsToExtractForLevels = new HashMap<Integer, List<ColumnToExtract>>(columnsToExtractForLevels);         
    }
    
    /**
     * Gets the row extractors factory for current nesting level of spreadsheet result hierarchy.
     * 
     * @param nestingLevel current nesting level
     * @return {@link NestedDataRowExtractorsFactory} for current nesting level
     */
    public NestedDataRowExtractorsFactory<Simple, Compound> getRowExtractorsFactory(int nestingLevel) {
        return new NestedDataRowExtractorsFactory<Simple, Compound>(getSimpleRowExtractor(nestingLevel), getCompoundRowExtractor(nestingLevel));
    }
    
    /**
     * 
     * @param nestingLevel current nesting level
     * @return list of columns that must be extracted on given level
     */
    public List<ColumnToExtract> getColumnsToExtract(int nestingLevel) {
        return new ArrayList<ColumnToExtract>(columnsToExtractForLevels.get(nestingLevel));
    }
    
    /**
     * Implement this method to return {@link RowExtractor} for the rows with only simple columns.
     * 
     * @param simpleExtractors extractors for simple columns
     * @return {@link RowExtractor} for the rows with only simple columns
     */
    protected abstract RowExtractor<Simple> initSimpleRowExtractor(List<SpreadsheetColumnExtractor<Simple>> simpleExtractors);
    
    /**
     * Implement this method to return {@link RowExtractor} for the rows contain compound columns.
     * 
     * @param compoundExtractors extractors for compound columns
     * @return {@link RowExtractor} for the rows contain compound columns
     */
    protected abstract RowExtractor<Compound> initCompoundRowExtractor(List<SpreadsheetColumnExtractor<Compound>> compoundExtractors);
    
    /**
     * Initialize the extractor for the column that is compound.
     * 
     * @param nestingLevel current level of nesting
     * @param column column for extraction
     * @param mandatory 
     * @return the extractor for the column that is compound.
     */
    protected NestedSpreadsheedColumnExtractor initCompoundColumnExtractor(int nestingLevel, ColumnToExtract column, boolean mandatory) {
    	return new NestedSpreadsheedColumnExtractor(nestingLevel, this, column, mandatory);
    }
    
    /**
     * 
     * Creates {@link RowExtractor} for rows that doesn`t have compound results.
     * @param nestingLevel current nesting level
     * @return {@link RowExtractor} for rows that doesn`t have compound results.
     */
    public RowExtractor<Simple> getSimpleRowExtractor(int nestingLevel) {
        /** column extractors for all columns that should be extracted in simple row*/ 
        List<SpreadsheetColumnExtractor<Simple>> simpleExtractors = new ArrayList<SpreadsheetColumnExtractor<Simple>>();
        List<ColumnToExtract> columns = columnsToExtractForLevels.get(nestingLevel);
        
        for (ColumnToExtract column : columns) {
            // create the column extractor for simple column value
            //
            simpleExtractors.add(new SpreadsheetColumnExtractor<Simple>(column, true));
        }
        return initSimpleRowExtractor(simpleExtractors);        
    }
    
    
    /** 
     * Creates {@link RowExtractor} for rows that have compound results.
     * 
     * @param nestingLevel current nesting level
     * @return {@link RowExtractor} for rows that have compound results.
     */
    @SuppressWarnings("unchecked")
	public RowExtractor<Compound> getCompoundRowExtractor(int nestingLevel) {
        /** column extractors for all columns that should be extracted in compound row*/
        List<SpreadsheetColumnExtractor<Compound>> compoundExtractors = 
            new ArrayList<SpreadsheetColumnExtractor<Compound>>();
        List<ColumnToExtract> columns = columnsToExtractForLevels.get(nestingLevel);
        for (ColumnToExtract column : columns) {
            if (column.containNested()) {
                // create column extractor for nested column value
                //
                compoundExtractors.add((SpreadsheetColumnExtractor<Compound>) initCompoundColumnExtractor(nestingLevel, column, true));
            } else {
                // not compound column values should be extracted also for compound rows.
                //
                compoundExtractors.add(new SpreadsheetColumnExtractor<Compound>(column, true));
            }
        }   
        return initCompoundRowExtractor(compoundExtractors);
    }
    
    public boolean containsNested(int nestingLevel) {
    	for (ColumnToExtract column : columnsToExtractForLevels.get(nestingLevel)) {
    		if (column.containNested()) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public List<ColumnToExtract> getCompoundColumnsToExtract(int nestingLevel) {
    	List<ColumnToExtract> compoundColumns = new ArrayList<ColumnToExtract>();
    	
    	for (ColumnToExtract column : columnsToExtractForLevels.get(nestingLevel)) {
    		if (column.containNested()) {
    			compoundColumns.add(column);
    		}
    	}
    	return compoundColumns;
    }
    

}
