package org.openl.rules.lang.xls.syntax;

import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;

public class HeaderSyntaxNode extends CellSyntaxNode {

    private static final String[] EMPTY_ARRAY = new String[] {};

    public static final String HEADER_TYPE = "org.openl.celltype.header";

    private final IdentifierNode headerToken;
    private final boolean isCollect;
    private final String[] collectParameters;

    public HeaderSyntaxNode(GridCellSourceCodeModule module, IdentifierNode headerToken) {
        this(module, headerToken, false, EMPTY_ARRAY);
    }

    public HeaderSyntaxNode(GridCellSourceCodeModule module,
            IdentifierNode headerToken,
            boolean isCollect,
            String[] collectParameters) {
        super(HEADER_TYPE, module);
        this.headerToken = headerToken;
        this.isCollect = isCollect;
        this.collectParameters = collectParameters;
    }

    public IdentifierNode getHeaderToken() {
        return headerToken;
    }

    public boolean isCollect() {
        return isCollect;
    }

    public String[] getCollectParameters() {
        return collectParameters;
    }

}
