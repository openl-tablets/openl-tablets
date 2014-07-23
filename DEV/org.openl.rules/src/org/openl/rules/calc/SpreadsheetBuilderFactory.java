package org.openl.rules.calc;

import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;

public class SpreadsheetBuilderFactory {
    private SpreadsheetBuilderFactory () {}
    
    public static SpreadsheetBuilder getSpreadsheetBuilder(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode, IOpenMethodHeader spreadsheetHeader) {
    	Boolean autoType =  tableSyntaxNode.getTableProperties().getAutoType();
        SpreadsheetComponentsBuilder componentBuilder = new SpreadsheetComponentsBuilder(tableSyntaxNode, bindingContext);
        SpreadsheetStructureBuilder structureBuilder = new SpreadsheetStructureBuilder(componentBuilder, spreadsheetHeader, autoType);

        SpreadsheetOpenClass spreadsheetOpenClass = new SpreadsheetOpenClass(null, spreadsheetHeader.getName() + "Type", bindingContext.getOpenL());
        SpreadsheetBuilder builder = new SpreadsheetBuilder(bindingContext, spreadsheetOpenClass);
        builder.setSpreadsheetCellsBuilder(structureBuilder);
        
        return builder;
    }
}
