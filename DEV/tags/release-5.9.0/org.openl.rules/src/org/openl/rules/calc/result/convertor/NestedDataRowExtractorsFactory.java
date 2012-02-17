package org.openl.rules.calc.result.convertor;

/**
 * Factory for Spreadsheet row extractors. There are 2 type of rows:<br>
 *  1) simple row, it means no SpreadsheetResult or SpreadsheetResult[] values in its columns<br>
 *  2) compound row, it means there is a nesting SpreadsheetResult or SpreadsheetResult[] value in one of the columns
 * 
 * @author DLiauchuk
 *
 */
public class NestedDataRowExtractorsFactory<Simple extends CodeStep, Compound extends CompoundStep> {
    
    /** row extractor for simple rows*/
    private RowExtractor<Simple> simpleRowExtractor;
    
    /** row extractor for  compound rows*/
    private RowExtractor<Compound> compoundRowExtractor; 
    
    public NestedDataRowExtractorsFactory(RowExtractor<Simple> simpleRowExtractor, 
            RowExtractor<Compound> compoundRowExtractor) {
        this.simpleRowExtractor = simpleRowExtractor;
        this.compoundRowExtractor = compoundRowExtractor;
    }
    
    /**
     * Gets the row extractor.
     * 
     * @param containsNested flag if the row contains any nested result
     * @return row extractor 
     */
    public RowExtractor<?> getRowExtractor(boolean containsNested) {
        if (containsNested) {
            return compoundRowExtractor;
        }
        return simpleRowExtractor;
    }
}
