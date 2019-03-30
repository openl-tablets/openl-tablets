package org.openl.rules.lang.xls;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

class TableSyntaxNodeCircularDependencyException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 4568066045442587374L;

    private TableSyntaxNode[] tableSyntaxNodes;

    public TableSyntaxNodeCircularDependencyException(TableSyntaxNode[] tableSyntaxNodes) {
        super(
            "Сustom Spreadsheet Type can't be defined correctly with the circular reference in spreadsheet table headers. Please, define manually common SpreadsheetResult type in spreadhseet table header.");
        this.tableSyntaxNodes = tableSyntaxNodes;
    }

    public TableSyntaxNode[] getTableSyntaxNodes() {
        return tableSyntaxNodes;
    }

}
