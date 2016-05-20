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

import org.openl.rules.calc.SpreadsheetResult;

/**
 * Column extractor for nesting spreadsheet values(e.g. SpreadsheetResult or SpreadsheetResult[])
 * 
 * @author DLiauchuk
 *
 */
@Deprecated
public class NestedSpreadsheedColumnExtractor extends SpreadsheetColumnExtractor<CompoundStep> {
    
	/**
	 * Indicates the current level of nesting.
	 */
    private int nestingLevel;
    
    /**
     * Configuration for each level of converting.
     */
    private NestedSpreadsheetConfiguration<?, ?> configuration;
    
    public NestedSpreadsheedColumnExtractor(int nestingLevel, NestedSpreadsheetConfiguration<?, ?> configuration, ColumnToExtract column, boolean mandatory) {
        super(column, mandatory);
        this.nestingLevel = nestingLevel;
        this.configuration = configuration;
    }
    
    public NestedSpreadsheedColumnExtractor (int nestingLevel, ColumnToExtract column, boolean mandatory) {
    	this(nestingLevel, null, column, mandatory);    	
    }
    
    /**
     * Gets the configuration 
     *  
     * @return {@link NestedSpreadsheetConfiguration}
     */
    public NestedSpreadsheetConfiguration<?, ?> getConfiguration() {
		return configuration;
	}
    
    /**
     * Setter for the {@link NestedSpreadsheetConfiguration}
     * 
     * @param configuration
     */
	public void setConfiguration(NestedSpreadsheetConfiguration<?, ?> configuration) {
		this.configuration = configuration;
	}
	
	/**
	 * Overrides the parent method.
	 * Analyze the 'from' object if it is a {@link SpreadsheetResult} or an array of it.
	 * According to this converts and stores the data to object 'to'.
	 */
	@Override
    public void convertAndStoreData(Object from, CompoundStep to) {
        if (from.getClass().isArray()) {
            // process SpreadsheetResult[] as nesting column value.
            //
            for (SpreadsheetResult result : (SpreadsheetResult[]) from) {            	
                CompoundStep compoundStep = configuration.getCompoundRowExtractor(nestingLevel).makeRowInstance();
                compoundStep.setSteps(convertCompoundPremium(result));
                
                // additional processing of converted results
                //
                postProcess(compoundStep);
                
                to.addStep(compoundStep);
            }
        } else {
            // process SpreadsheetResult as nesting column value.
            //
            to.setSteps(convertCompoundPremium((SpreadsheetResult) from));
        }
        // additional processing of converted results
        //
        postProcess(to);
    }
    
	/**
	 * Override this method for your purpose, if there is a need in 
	 * additional processing of converted result
	 * 
	 * @param compoundStep already converted result to {@link CompoundStep}
	 */
    protected void postProcess(CompoundStep compoundStep) {
    	// Default implementation. Do nothing.
    	// Override it for you purpose		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private NestedSpreadsheetResultConverter<?, ?> createNextLevelConverter() {
        return new NestedSpreadsheetResultConverter(nestingLevel + 1, configuration);
    }
    
    private List<CodeStep> convertCompoundPremium(SpreadsheetResult result) {    
        NestedSpreadsheetResultConverter<?, ?> converter = createNextLevelConverter();
        return converter.process(result);
    }

}
