package org.openl.rules.diff.xls2;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.source.IOpenSourceCodeModule;

public class XlsTable {
    private TableSyntaxNode node;
    private IOpenLTable table;

    public XlsTable(TableSyntaxNode node) {
        this.node = node;
        table = new TableSyntaxNodeAdapter(node);
    }

    public String getSheetName() {
        IOpenSourceCodeModule sheet = node.getModule();
        String sheetName = ((XlsSheetSourceCodeModule) sheet).getSheetName();
        return sheetName;
    }

    public String getTableName() {
        String header = table.getGridTable().getCell(0, 0).getStringValue();
        String tableName = (header == null) ? "" : header;
        return tableName;
    }

    public GridLocation getLocation() {
        GridLocation location = node.getGridLocation();
        return location;
    }

    public IOpenLTable getTable() {
        return table;
    }
}
