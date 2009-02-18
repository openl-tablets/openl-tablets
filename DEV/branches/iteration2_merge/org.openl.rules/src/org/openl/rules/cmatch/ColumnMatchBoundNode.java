package org.openl.rules.cmatch;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

public class ColumnMatchBoundNode extends AMethodBasedNode implements IMemberBoundNode, IXlsTableNames {
    private final IOpenSourceCodeModule nameOfAlgorithm;

    public ColumnMatchBoundNode(TableSyntaxNode tsn, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module,
            IOpenSourceCodeModule nameOfAlgorithm) {
        super(tsn, openl, header, module);

        this.nameOfAlgorithm = nameOfAlgorithm;
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        ColumnMatchBuilder builder = new ColumnMatchBuilder(cxt, getColumnMatch(), getTableSyntaxNode());

        ILogicalTable tableBody = this.getTableSyntaxNode().getTableBody();
        getTableSyntaxNode().getSubTables().put(VIEW_BUSINESS, tableBody.rows(1));
        builder.build(tableBody);
    }

    @Override
    protected IOpenMethod createMethodShell() {
        return new ColumnMatch(header, this);
    }

    public ColumnMatch getColumnMatch() {
        return (ColumnMatch) method;
    }

    public IOpenSourceCodeModule getAlgorithm() {
        return nameOfAlgorithm;
    }
}
