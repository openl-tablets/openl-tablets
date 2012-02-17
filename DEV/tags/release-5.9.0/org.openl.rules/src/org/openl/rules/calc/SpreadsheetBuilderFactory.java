package org.openl.rules.calc;

import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class SpreadsheetBuilderFactory {
    private SpreadsheetBuilderFactory () {}
    
    public static SpreadsheetBuilder getSpreadsheetBuilder(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode) {
        SpreadsheetComponentsBuilder componentBuilder = getComponentBuilder(bindingContext, tableSyntaxNode);
        SpreadsheetStructureBuilder structureBuilder = getStructureBuilder(componentBuilder);
        
        SpreadsheetBuilder builder = new SpreadsheetBuilder(bindingContext);
        builder.setSpreadsheetCellsBuilder(structureBuilder);
        
        return builder;
    }

    public static SpreadsheetStructureBuilder getStructureBuilder(SpreadsheetComponentsBuilder componentBuilder) {        
        return new SpreadsheetStructureBuilder(componentBuilder);
    }

    public static SpreadsheetComponentsBuilder getComponentBuilder(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode) {
        return new SpreadsheetComponentsBuilder(tableSyntaxNode, bindingContext);
    }
}
