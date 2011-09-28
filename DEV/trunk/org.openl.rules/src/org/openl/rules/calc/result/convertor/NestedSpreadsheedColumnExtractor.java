package org.openl.rules.calc.result.convertor;

import java.util.List;

import org.openl.rules.calc.SpreadsheetResult;

/**
 * Column extractor for nesting spreadsheet values(e.g. SpreadsheetResult or SpreadsheetResult[])
 * 
 * @author DLiauchuk
 *
 */
public class NestedSpreadsheedColumnExtractor extends SpreadsheetColumnExtractor<CompoundStep> {
    
    private int nestingLevel;
    
    private NestedSpreadsheetConfiguration<?, ?> configuration;
    
    public NestedSpreadsheedColumnExtractor(int nestingLevel, NestedSpreadsheetConfiguration<?, ?> configuration, ColumnToExtract column, boolean mandatory) {
        super(column, mandatory);
        this.nestingLevel = nestingLevel;
        this.configuration = configuration;
    }
    
    public NestedSpreadsheedColumnExtractor (int nestingLevel, ColumnToExtract column, boolean mandatory) {
    	this(nestingLevel, null, column, mandatory);    	
    }
    
    public NestedSpreadsheetConfiguration<?, ?> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(NestedSpreadsheetConfiguration<?, ?> configuration) {
		this.configuration = configuration;
	}

	@Override
    public void convertAndStoreData(Object from, CompoundStep to) {
        if (from.getClass().isArray()) {
            // process SpreadsheetResult[] as nesting column value.
            //
            for (SpreadsheetResult result : (SpreadsheetResult[]) from) {
                CompoundStep compoundStep = new CompoundStep();
                compoundStep.setSteps(convertCompoundPremium(result));
                to.addStep(compoundStep);
            }
        } else {
            // process SpreadsheetResult as nesting column value.
            //
            to.setSteps(convertCompoundPremium((SpreadsheetResult) from));
        }
    }
    
    @SuppressWarnings("unchecked")
	private NestedSpreadsheetResultConverter<?, ?> createNextLevelConverter() {
        return new NestedSpreadsheetResultConverter(nestingLevel + 1, configuration);
    }
    
    private List<CodeStep> convertCompoundPremium(SpreadsheetResult result) {    
        NestedSpreadsheetResultConverter<?, ?> converter = createNextLevelConverter();
        return converter.process(result);
    }

}
