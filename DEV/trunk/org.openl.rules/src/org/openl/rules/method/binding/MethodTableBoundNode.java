/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.method.binding;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.openl.GridTableSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

/**
 * @author snshor
 *
 */
public class MethodTableBoundNode extends AMethodBasedNode {

    class TableMethod extends CompositeMethod implements IMemberMetaInfo {

        /**
         * @param header
         * @param methodBodyBoundNode
         */
        public TableMethod(IOpenMethodHeader header, IBoundMethodNode methodBodyBoundNode) {
            super(header, methodBodyBoundNode);
        }

        public BindingDependencies getDependencies() {
            BindingDependencies bd = new BindingDependencies();
            updateDependency(bd);
            return bd;
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return this;
        }

        public String getSourceUrl() {
            return getTableSyntaxNode().getUri();
        }

        public ISyntaxNode getSyntaxNode() {
            return MethodTableBoundNode.this.getSyntaxNode();
        }

    }

    TableMethod xx;

    /**
     * @param syntaxNode
     * @param children
     */
    public MethodTableBoundNode(TableSyntaxNode methodNode, OpenL openl, IOpenMethodHeader header,
            ModuleOpenClass module) {
        super(methodNode, openl, header, module);

    }

    @Override
    protected IOpenMethod createMethodShell() {
        return new TableMethod(header, null);
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {

        TableSyntaxNode tsn = getTableSyntaxNode();

        ILogicalTable lt = tsn.getTable();

        int expectedHeight = tsn.getTableProperties() == null ? 2 : 3;

        if (lt.getLogicalHeight() != expectedHeight) {
            throw new BoundError(null,
                    "Method table must have 2 row cells, and one optional property row: <header> [properties] <body>",
                    null, new GridTableSourceCodeModule(lt.getGridTable()));
        }

        IOpenSourceCodeModule src = new GridCellSourceCodeModule(lt.getLogicalRow(expectedHeight - 1).getGridTable());

        OpenlTool.compileMethod(src, openl, getTableMethod(), cxt);

        // method.setMethodBodyBoundNode(methodBody.getMethodBodyBoundNode());

    }

    final public TableMethod getTableMethod() {
        return (TableMethod) method;
    }

    @Override
    public IOpenClass getType() {
        return header.getType();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        getTableMethod().getMethodBodyBoundNode().updateDependency(dependencies);
    }

}
