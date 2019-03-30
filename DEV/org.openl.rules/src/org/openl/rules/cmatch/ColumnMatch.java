package org.openl.rules.cmatch;

import java.util.List;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

@Executable
public class ColumnMatch extends ExecutableRulesMethod {
    private List<TableColumn> columns;
    private List<TableRow> rows;

    private Object[] returnValues;
    private MatchNode checkTree;

    private IMatchAlgorithmExecutor algorithmExecutor;

    // WEIGHT algorithm
    private MatchNode totalScore;
    private int[] columnScores;

    public ColumnMatch() {
        super(null, null);
    }
    
    public ColumnMatch(IOpenMethodHeader header, ColumnMatchBoundNode node) {
        super(header, node);
        initProperties(getSyntaxNode().getTableProperties());
    }

    public IOpenSourceCodeModule getAlgorithm() {
        return ((ColumnMatchBoundNode) getBoundNode()).getAlgorithm();
    }

    public MatchNode getCheckTree() {
        return checkTree;
    }

    public List<TableColumn> getColumns() {
        return columns;
    }

    public int[] getColumnScores() {
        return columnScores;
    }

    @Override
    public BindingDependencies getDependencies() {
        BindingDependencies dependencies = new RulesBindingDependencies();
        getBoundNode().updateDependency(dependencies);
        return dependencies;
    }

    public Object[] getReturnValues() {
        return returnValues;
    }

    public List<TableRow> getRows() {
        return rows;
    }

    @Override
    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public MatchNode getTotalScore() {
        return totalScore;
    }

    @Override
    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        Object result = algorithmExecutor.invoke(this, params, env);
        if (result == null) {
            Class<?> type = getHeader().getType().getInstanceClass();
            if (type.isPrimitive()) {
                throw new IllegalArgumentException("Cannot return <null> for primitive type " + type.getName());
            }
        }
        return result;
    }

    public void setAlgorithmExecutor(IMatchAlgorithmExecutor algorithmExecutor) {
        this.algorithmExecutor = algorithmExecutor;
    }

    public void setCheckTree(MatchNode checkTree) {
        this.checkTree = checkTree;
    }

    public void setColumns(List<TableColumn> columns) {
        this.columns = columns;
    }

    public void setColumnScores(int[] columnScores) {
        this.columnScores = columnScores;
    }

    public void setReturnValues(Object[] returnValues) {
        this.returnValues = returnValues;
    }

    public void setRows(List<TableRow> rows) {
        this.rows = rows;
    }

    public void setTotalScore(MatchNode totalScore) {
        this.totalScore = totalScore;
    }

    public IMatchAlgorithmExecutor getAlgorithmExecutor() {
        return algorithmExecutor;
    }

}
