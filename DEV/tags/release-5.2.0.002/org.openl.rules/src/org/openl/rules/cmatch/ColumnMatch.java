package org.openl.rules.cmatch;

import java.util.List;

import org.openl.IOpenSourceCodeModule;
import org.openl.binding.BindingDependencies;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
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

    private IMatchAlgorithmExecutor algorithmExecutor;

    // WEIGHT algorithm
    private MatchNode totalScore;
    private int[] columnScores;

    public ColumnMatch(IOpenMethodHeader header, ColumnMatchBoundNode node) {
        super(header);
        this.node = node;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Object result = algorithmExecutor.invoke(target, params, env, this);

        // FIXME ?null or exception?
        return result;
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

    public void setAlgorithmExecutor(IMatchAlgorithmExecutor algorithmExecutor) {
        this.algorithmExecutor = algorithmExecutor;
    }

    public IOpenSourceCodeModule getAlgorithm() {
        return node.getAlgorithm();
    }

    public MatchNode getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(MatchNode totalScore) {
        this.totalScore = totalScore;
    }

    public int[] getColumnScores() {
        return columnScores;
    }

    public void setColumnScores(int[] columnScores) {
        this.columnScores = columnScores;
    }

}
