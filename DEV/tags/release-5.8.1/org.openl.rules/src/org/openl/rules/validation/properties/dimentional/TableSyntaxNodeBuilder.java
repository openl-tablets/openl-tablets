package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.impl.IdentifierNode;

public class TableSyntaxNodeBuilder {
    
    private String tableType;
    
    private XlsSheetGridModel sheetGridModel;
    
    private IGridTable gridTable;
    
    public TableSyntaxNodeBuilder(String tableType, XlsSheetGridModel sheetGridModel, IGridTable gridTable) {
        this.tableType = tableType;
        this.sheetGridModel = sheetGridModel;
        this.gridTable = gridTable;
    }
    
    public TableSyntaxNode build() {
        GridLocation pos = new GridLocation(gridTable);
        HeaderSyntaxNode headerSyntaxNode = new HeaderSyntaxNode(null, new IdentifierNode(null, null, "Rules", null));

        return new TableSyntaxNode(tableType, pos, sheetGridModel.getSheetSource(), gridTable, headerSyntaxNode);
    }
}
