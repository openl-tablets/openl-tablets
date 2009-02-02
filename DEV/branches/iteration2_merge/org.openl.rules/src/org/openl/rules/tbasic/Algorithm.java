package org.openl.rules.tbasic;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class Algorithm extends AMethod implements IMemberMetaInfo {
    private final AlgorithmBoundNode node;

    private final List<AlgorithmRow> rows;

    public Algorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        super(header);
        this.node = node;

        rows = new ArrayList<AlgorithmRow>();
    }

    public static Algorithm createAlgorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        return new Algorithm(header, node);
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return null;
        // return new AlgorithmResult(this, (IDynamicObject) target, params,
        // env);
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

    public void addRow(AlgorithmRow row) {
        rows.add(row);
    }

    public List<AlgorithmRow> getRows() {
        return rows;
    }
}
