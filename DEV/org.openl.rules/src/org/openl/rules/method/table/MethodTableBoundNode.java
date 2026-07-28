package org.openl.rules.method.table;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;

/**
 * @author snshor
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
        super.finalizeBind(bindingContext);

        var tsn = getTableSyntaxNode();

        var logicalTable = tsn.getTable();
        var tableHasProperties = tsn.hasPropertiesDefinedInTable();
        var bodyTable = logicalTable.getRows(tableHasProperties ? 2 : 1);

        if (bodyTable == null) {
            var errorMessage = "Body section is mandatory for Method table.";
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(errorMessage, tsn);
            bindingContext.addError(error);
        } else {
            var height = bodyTable.getHeight();

            IOpenSourceCodeModule[] cellSources = new IOpenSourceCodeModule[height];

            for (var i = 0; i < height; i++) {
                cellSources[i] = new GridCellSourceCodeModule(bodyTable.getRow(i).getSource(), bindingContext);
            }

            var src = new CompositeSourceCodeModule(cellSources, "\n");

            OpenLManager.compileMethod(getOpenl(), src, getTableMethod().getCompositeMethod(), bindingContext);
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
}
