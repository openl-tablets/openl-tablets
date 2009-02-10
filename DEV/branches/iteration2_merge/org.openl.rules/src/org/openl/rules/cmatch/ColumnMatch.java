package org.openl.rules.cmatch;

import java.util.List;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class ColumnMatch extends AMethod implements IMemberMetaInfo {
    private final ColumnMatchBoundNode node;

    private List<TableColumn> columns;
    private List<TableRow> rows;

    private IOpenClass thisClass;

    public ColumnMatch(IOpenMethodHeader header, ColumnMatchBoundNode node) {
        super(header);
        this.node = node;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
//        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(thisClass, (IDynamicObject) target);
//
//        TBasicVM columnMatchVM = new TBasicVM(algorithmSteps, labels);
//        return columnMatchVM.run(thisInstance, target, params, env);

        // FIXME
        node.getAlgorithm().compile(this);
        return null;
    }

    public BindingDependencies getDependencies() {
        // TODO Auto-generated method stub
        return null;
    }

    public ISyntaxNode getSyntaxNode() {
        return node.getSyntaxNode();
    }

    public String getSourceUrl() {
        return ((TableSyntaxNode) node.getSyntaxNode()).getUri();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public List<TableColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<TableColumn> columns) {
        this.columns = columns;
    }

    public List<TableRow> getRows() {
        return rows;
    }

    public void setRows(List<TableRow> rows) {
        this.rows = rows;
    }

    public void setThisClass(IOpenClass thisClass) {
        this.thisClass = thisClass;
    }
}