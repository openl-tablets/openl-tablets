package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.syntax.impl.IdentifierNode;

public class TableSyntaxNodeBuilder implements Builder<TableSyntaxNode>{
    
    private String tableType;    
    private XlsSheetSourceCodeModule sheetSource;    
    private IGridTable gridTable;
    
    public TableSyntaxNodeBuilder(String tableType, XlsSheetSourceCodeModule sheetSource, IGridTable gridTable) {
        this.tableType = tableType;
        this.sheetSource = sheetSource;
        this.gridTable = gridTable;
    }
    
    public TableSyntaxNode build() {
        GridLocation pos = new GridLocation(gridTable);
        
        return new TableSyntaxNode(tableType, pos, sheetSource, gridTable, buildHeader());
    }
    
    protected HeaderSyntaxNode buildHeader() {
    	return new HeaderSyntaxNode(null, new IdentifierNode(null, null, "Rules", null)); 
    }
}
