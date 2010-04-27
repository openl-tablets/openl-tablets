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

public class AlgorithmBoundNode extends AMethodBasedNode implements IMemberBoundNode {

    public AlgorithmBoundNode(TableSyntaxNode tsn, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module) {
        super(tsn, openl, header, module);
    }

    @Override
    protected IOpenMethod createMethodShell() {
        return Algorithm.createAlgorithm(getHeader(), this);
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        AlgorithmBuilder builder = new AlgorithmBuilder(cxt, getAlgorithm(), getTableSyntaxNode());

        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();
        builder.build(tableBody);

        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody.rows(1));
    }

    public Algorithm getAlgorithm() {
        return (Algorithm) getMethod();
    }
}
