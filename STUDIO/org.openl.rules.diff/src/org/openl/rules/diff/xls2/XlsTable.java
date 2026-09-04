package org.openl.rules.diff.xls2;

import lombok.Getter;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.syntax.GridLocation;

public class XlsTable {
    private final TableSyntaxNode node;
    @Getter
    private final IOpenLTable table;

    public XlsTable(TableSyntaxNode node) {
        this.node = node;
        table = new TableSyntaxNodeAdapter(node);
    }

    public String getSheetName() {
        var sheet = node.getModule();
        return ((XlsSheetSourceCodeModule) sheet).getSheetName();
    }

    public String getTableName() {
        var header = table.getGridTable().getCell(0, 0).getStringValue();
        return header == null ? "" : header;
    }

    public GridLocation getLocation() {
        return node.getGridLocation();
    }
}
