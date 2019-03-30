/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.method.table;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.MethodTableMetaInfoReader;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
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
    protected ExecutableRulesMethod createMethodShell() {
        return new TableMethod(getHeader(), null, this);
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        if (!bindingContext.isExecutionMode()) {
            getTableSyntaxNode().setMetaInfoReader(new MethodTableMetaInfoReader(this));
        }

        super.finalizeBind(bindingContext);

        TableSyntaxNode tsn = getTableSyntaxNode();

        ILogicalTable logicalTable = tsn.getTable();
        boolean tableHasProperties = tsn.hasPropertiesDefinedInTable();
        ILogicalTable bodyTable = logicalTable.getRows(tableHasProperties ? 2 : 1);

        if (bodyTable == null) {
            String errorMessage = "Method table must contain a body section";
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(errorMessage, tsn);
            tsn.addError(error);
            bindingContext.addError(error);
        } else {
            int height = bodyTable.getHeight();

            IOpenSourceCodeModule[] cellSources = new IOpenSourceCodeModule[height];

            for (int i = 0; i < height; i++) {
                cellSources[i] = new GridCellSourceCodeModule(bodyTable.getRow(i).getSource(), bindingContext);
            }

            IOpenSourceCodeModule src = new CompositeSourceCodeModule(cellSources, "\n");

            OpenLCellExpressionsCompiler
                .compileMethod(getOpenl(), src, getTableMethod().getCompositeMethod(), bindingContext);
        }
    }

    @Override
    public IOpenClass getType() {
        return getHeader().getType();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        getTableMethod().getCompositeMethod().getMethodBodyBoundNode().updateDependency(dependencies);
    }

    private TableMethod getTableMethod() {
        return (TableMethod) getMethod();
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        super.removeDebugInformation(cxt);
        getTableMethod().getCompositeMethod().removeDebugInformation();
    }
}
