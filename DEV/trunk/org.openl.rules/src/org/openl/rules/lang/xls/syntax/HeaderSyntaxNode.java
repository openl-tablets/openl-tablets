package org.openl.rules.lang.xls.syntax;

import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;

public class HeaderSyntaxNode extends CellSyntaxNode {

    public static final String HEADER_TYPE = "org.openl.celltype.header";

    private IdentifierNode headerToken;

    public HeaderSyntaxNode(GridCellSourceCodeModule module, IdentifierNode headerToken) {
        super(HEADER_TYPE, module);
        this.headerToken = headerToken;
    }

    public IdentifierNode getHeaderToken() {
        return headerToken;
    }

}
