package org.openl.rules.lang.xls.syntax;

import lombok.Getter;

import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;

public class HeaderSyntaxNode extends CellSyntaxNode {

    private static final String[] EMPTY_ARRAY = new String[]{};

    public static final String HEADER_TYPE = "org.openl.celltype.header";

    @Getter
    private final IdentifierNode headerToken;
    @Getter
    private final boolean isCollect;
    @Getter
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

}
