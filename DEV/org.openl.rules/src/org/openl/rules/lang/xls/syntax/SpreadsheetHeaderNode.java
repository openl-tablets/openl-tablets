package org.openl.rules.lang.xls.syntax;

import org.openl.rules.calc.CellsHeaderExtractor;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;

public class SpreadsheetHeaderNode extends HeaderSyntaxNode {

    private CellsHeaderExtractor cellHeadersExtractor;

    public SpreadsheetHeaderNode(GridCellSourceCodeModule module, IdentifierNode headerToken) {
        super(module, headerToken);
    }

    public CellsHeaderExtractor getCellHeadersExtractor() {
        return cellHeadersExtractor;
    }

    public void setCellHeadersExtractor(CellsHeaderExtractor cellHeadersExtractor) {
        this.cellHeadersExtractor = cellHeadersExtractor;
    }
}
