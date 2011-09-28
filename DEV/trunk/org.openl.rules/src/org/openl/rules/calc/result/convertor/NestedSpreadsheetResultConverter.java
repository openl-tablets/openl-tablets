package org.openl.rules.calc.result.convertor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.calc.SpreadsheetResult;

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
 * SpreadsheetResult convertor that supports nested SpreadsheetResult as column values.
 * Converts the SpreadsheetResult to flat structure.
 * 
 * @author DLiauchuk
 * 
 * @param <Simple> class that will be populated with values, when extracting rows without compound results.
 * @param <Compound> class that will be populated with values, when extracting rows wit compound results.
 */
public class NestedSpreadsheetResultConverter<Simple extends CodeStep, Compound extends CompoundStep> {
    
    private static final Log LOG = LogFactory.getLog(NestedSpreadsheetResultConverter.class);
       
    private NestedDataRowExtractorsFactory<Simple, Compound> rowExtractorsFactory;
    
    private NestedSpreadsheetConfiguration<Simple, Compound> conf;
    
    private int currentNestingLevel;
    
    /**
     * 
     * @param currentNestingLevel the number of the current nesting level
     * @param configuration configuration that is used for extracting rows on this and further levels, connat be null.
     * In that case will throw {@link IllegalArgumentException}
     */
    public NestedSpreadsheetResultConverter(int currentNestingLevel, NestedSpreadsheetConfiguration<Simple, Compound> configuration) {
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
     * @param spreadsheetResult {@link SpreadsheetResult} that is going to be converted.
     * 
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
        LOG.warn("Spreadsheet result is null");
        return steps;
    }

    private CodeStep processRow(SpreadsheetResult spreadsheetResult, int row) {          
        CodeStep step = null;
        
        RowExtractor<?> rowExtractor = rowExtractorsFactory.getRowExtractor(conf.containsNested(currentNestingLevel));
        step = rowExtractor.extract(spreadsheetResult, row);
        
        step.setStepName(spreadsheetResult.getRowName(row));
        return step;
    }
 
}
