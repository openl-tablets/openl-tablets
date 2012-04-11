package org.openl.rules.tbasic;

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

public class AlgorithmBoundNode extends AMethodBasedNode implements IMemberBoundNode, IXlsTableNames {

    public AlgorithmBoundNode(TableSyntaxNode tsn, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module) {
        super(tsn, openl, header, module);
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        AlgorithmBuilder builder = new AlgorithmBuilder(cxt, getAlgorithm(), getTableSyntaxNode());

        ILogicalTable tableBody = this.getTableSyntaxNode().getTableBody();
        getTableSyntaxNode().getSubTables().put(VIEW_BUSINESS, tableBody.rows(1));
        builder.build(tableBody);
    }

    @Override
    protected IOpenMethod createMethodShell() {
        return Algorithm.createAlgorithm(header, this);
    }

    public Algorithm getAlgorithm() {
        return (Algorithm) method;
    }
}
