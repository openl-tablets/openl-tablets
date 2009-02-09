package org.openl.rules.cmatch;

import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.impl.SyntaxError;

public class ColumnMatchBuilder {
    private final IBindingContext bindingContext;
    private final ColumnMatch columnMatch;
    private final TableSyntaxNode tsn;

    public ColumnMatchBuilder(IBindingContext ctx, ColumnMatch columnMatch, TableSyntaxNode tsn) {
        this.bindingContext = ctx;
        this.columnMatch = columnMatch;
        this.tsn = tsn;
    }

    public void build(ILogicalTable tableBody) throws Exception {
        if (tableBody.getLogicalHeight() <= 2) {
            throw new SyntaxError(tsn, "Unsufficient rows. Must be more than 2!", null);
        }

        // TODO
    }
}
