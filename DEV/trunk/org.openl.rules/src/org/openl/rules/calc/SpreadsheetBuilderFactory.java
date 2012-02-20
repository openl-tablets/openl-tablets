package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class SpreadsheetBuilderFactory {
    private SpreadsheetBuilderFactory () {}
    
    public static SpreadsheetBuilder getSpreadsheetBuilder(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode, String spreadsheetName) {
        SpreadsheetComponentsBuilder componentBuilder = getComponentBuilder(bindingContext, tableSyntaxNode);
        SpreadsheetStructureBuilder structureBuilder = getStructureBuilder(componentBuilder);
        
        SpreadsheetBuilder builder = new SpreadsheetBuilder(bindingContext, getSpreadsheetOpenClass(bindingContext.getOpenL(), spreadsheetName));
        builder.setSpreadsheetCellsBuilder(structureBuilder);
        
        return builder;
    }

    public static SpreadsheetStructureBuilder getStructureBuilder(SpreadsheetComponentsBuilder componentBuilder) {        
        return new SpreadsheetStructureBuilder(componentBuilder);
    }

    public static SpreadsheetComponentsBuilder getComponentBuilder(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode) {
        return new SpreadsheetComponentsBuilder(tableSyntaxNode, bindingContext);
    }
    
    public static SpreadsheetOpenClass getSpreadsheetOpenClass(OpenL openl, String spreadsheetName) {        
        return new SpreadsheetOpenClass(null, spreadsheetName + "Type", openl);
        
    }
}
