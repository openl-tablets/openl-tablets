package org.openl.rules.calc;

import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class SpreadsheetBuilderFactory {
    private SpreadsheetBuilderFactory () {}
    
    public static SpreadsheetBuilder getSpreadsheetBuilder(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode, String spreadsheetName) {
        SpreadsheetComponentsBuilder componentBuilder = new SpreadsheetComponentsBuilder(tableSyntaxNode, bindingContext);
        SpreadsheetStructureBuilder structureBuilder = new SpreadsheetStructureBuilder(componentBuilder);

        SpreadsheetOpenClass spreadsheetOpenClass = new SpreadsheetOpenClass(null, spreadsheetName + "Type", bindingContext.getOpenL());
        SpreadsheetBuilder builder = new SpreadsheetBuilder(bindingContext, spreadsheetOpenClass);
        builder.setSpreadsheetCellsBuilder(structureBuilder);
        
        return builder;
    }
}
