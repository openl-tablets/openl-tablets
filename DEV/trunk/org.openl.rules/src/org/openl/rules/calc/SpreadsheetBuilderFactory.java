package org.openl.rules.calc;

import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class SpreadsheetBuilderFactory {
    private SpreadsheetBuilderFactory () {}
    
    public static SpreadsheetBuilder getSpreadsheetBuilder(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode) {
        SpreadsheetSourceExtractor sourceExtractor = getSourceExtractor(bindingContext, tableSyntaxNode);
        SpreadsheetStructureBuilder structureBuilder = getStructureBuilder(sourceExtractor);
        
        SpreadsheetBuilder builder = new SpreadsheetBuilder(bindingContext);
        builder.setSpreadsheetCellsBuilder(structureBuilder);
        
        return builder;
    }

    public static SpreadsheetStructureBuilder getStructureBuilder(SpreadsheetSourceExtractor sourceExtractor) {        
        return new SpreadsheetStructureBuilder(sourceExtractor);
    }

    public static SpreadsheetSourceExtractor getSourceExtractor(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode) {
        return new SpreadsheetSourceExtractor(tableSyntaxNode, bindingContext);
    }
}
