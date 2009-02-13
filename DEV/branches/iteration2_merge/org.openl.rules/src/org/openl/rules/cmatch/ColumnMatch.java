package org.openl.rules.cmatch;

import java.util.List;

import org.openl.binding.BindingDependencies;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class ColumnMatch extends AMethod implements IMemberMetaInfo {
    private final ColumnMatchBoundNode node;

    private List<TableColumn> columns;
    private List<TableRow> rows;

    private Object[] returnValues;
    private MatchNode checkTree;

    public ColumnMatch(IOpenMethodHeader header, ColumnMatchBoundNode node) {
        super(header);
        this.node = node;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        // FIXME
        // MOVE UNDER ALGORITHM

        for (MatchNode node : checkTree.getChildren()) {
            Argument arg = node.getArgument();
            Object var = arg.extractValue(target, params, env);
            IMatcher matcher = node.getMatcher();

            for (int i = 0; i < returnValues.length; i++) {
                Object checkValue = node.getCheckValues()[i];
                if (matcher.match(var, checkValue)) {
                    // check that all children are MATCH at i-th element
                    if (childrenMatch(target, params, env, node, i)) {
                        return returnValues[i];
                    }
                }
            }
        }

        // FIXME ?null or exception?
        return null;
    }

    protected boolean childrenMatch(Object target, Object[] params, IRuntimeEnv env, MatchNode parent, int index) {
        for (MatchNode node : parent.getChildren()) {
            Argument arg = node.getArgument();
            Object var = arg.extractValue(target, params, env);
            IMatcher matcher = node.getMatcher();

            Object checkValue = node.getCheckValues()[index];
            if (matcher.match(var, checkValue)) {
                // check that all children are MATCH at i-th element
                if (!childrenMatch(target, params, env, node, index)) {
                    return false;
                }
            } else {
                // fail fast
                return false;
            }
        }

        // all TRUE or no children
        return true;
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

    public ColumnMatchAlgorithm getAlgorithm() {
        return node.getAlgorithm();
    }

    public Object[] getReturnValues() {
        return returnValues;
    }

    public void setReturnValues(Object[] returnValues) {
        this.returnValues = returnValues;
    }

    public MatchNode getCheckTree() {
        return checkTree;
    }

    public void setCheckTree(MatchNode checkTree) {
        this.checkTree = checkTree;
    }
}
