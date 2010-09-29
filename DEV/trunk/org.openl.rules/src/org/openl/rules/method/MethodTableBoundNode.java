/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.method;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

/**
 * @author snshor
 * 
 */
public class MethodTableBoundNode extends AMethodBasedNode {

    public MethodTableBoundNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IOpenMethodHeader header,
            ModuleOpenClass module) {

        super(tableSyntaxNode, openl, header, module);
    }

    @Override
    protected IOpenMethod createMethodShell() {
        return new TableMethod(getHeader(), null, this);
    }

    public void finalizeBind(IBindingContext bindingContext) throws Exception {

        TableSyntaxNode tsn = getTableSyntaxNode();

        ILogicalTable logicalTable = tsn.getTable();
        boolean tableHasProperties = tsn.hasPropertiesDefinedInTable();
        ILogicalTable bodyTable = logicalTable.rows(tableHasProperties ? 2 : 1);

        int height = bodyTable.getLogicalHeight();

        IOpenSourceCodeModule[] cellSources = new IOpenSourceCodeModule[height];

        for (int i = 0; i < height; i++) {
            cellSources[i] = new GridCellSourceCodeModule(bodyTable.getLogicalRow(i).getGridTable(), bindingContext);
        }

        IOpenSourceCodeModule src = new CompositeSourceCodeModule(cellSources, "\n");

        OpenLManager.compileMethod(getOpenl(), src, getTableMethod(), bindingContext);
        if (bindingContext.isExecutionMode()) {
            getTableMethod().setMethodTableBoundNode(null);
        }
    }

    @Override
    public IOpenClass getType() {
        return getHeader().getType();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        getTableMethod().getMethodBodyBoundNode().updateDependency(dependencies);
    }

    private TableMethod getTableMethod() {
        return (TableMethod) getMethod();
    }

}
