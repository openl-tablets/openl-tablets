package org.openl.rules.lang.xls.syntax;

import java.util.Objects;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;

public class HeaderSyntaxNode extends CellSyntaxNode {

    private static final String[] EMPTY_ARRAY = new String[]{};

    public static final String HEADER_TYPE = "org.openl.celltype.header";

    private final IOpenSourceCodeModule source;
    @Getter
    private final IdentifierNode headerToken;
    @Getter
    private final boolean isCollect;
    @Getter
    private final String[] collectParameters;

    public HeaderSyntaxNode(@NonNull GridCellSourceCodeModule module, IdentifierNode headerToken) {
        this(module, headerToken, false, EMPTY_ARRAY);
    }

    public HeaderSyntaxNode(@NonNull GridCellSourceCodeModule module,
                            IdentifierNode headerToken,
                            boolean isCollect,
                            String[] collectParameters) {
        super(HEADER_TYPE, module);
        this.source = Objects.requireNonNull(module, "module");
        this.headerToken = headerToken;
        this.isCollect = isCollect;
        this.collectParameters = collectParameters;
    }

    /**
     * Returns the text of the header cell.
     *
     * <p>A header is always read from its cell, so the text is never {@code null}. An empty cell gives an empty
     * string.
     */
    @Override
    public @NonNull String getSourceString() {
        return source.getCode();
    }

}
