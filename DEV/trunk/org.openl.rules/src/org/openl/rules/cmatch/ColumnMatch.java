package org.openl.rules.cmatch;

import java.util.List;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

@Executable
public class ColumnMatch extends ExecutableRulesMethod {
    private ColumnMatchBoundNode boundNode;

    private List<TableColumn> columns;
    private List<TableRow> rows;

    private Object[] returnValues;
    private MatchNode checkTree;

    private IMatchAlgorithmExecutor algorithmExecutor;
    
    private Invokable invoker;

    // WEIGHT algorithm
    private MatchNode totalScore;
    private int[] columnScores;    

    public ColumnMatch(IOpenMethodHeader header, ColumnMatchBoundNode node) {
        super(header);
        this.boundNode = node;      
        initProperties(getSyntaxNode().getTableProperties());
    }
    
    public ColumnMatchBoundNode getBoundNode() {
        return boundNode;
    }

    public void setBoundNode(ColumnMatchBoundNode boundNode) {
        this.boundNode = boundNode;
    }

    public IOpenSourceCodeModule getAlgorithm() {
        return boundNode.getAlgorithm();
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

    public BindingDependencies getDependencies() {
        BindingDependencies dependencies = new RulesBindingDependencies();
        boundNode.updateDependency(dependencies);
        return dependencies;
    }

    public Object[] getReturnValues() {
        return returnValues;
    }

    public List<TableRow> getRows() {
        return rows;
    }

    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public TableSyntaxNode getSyntaxNode() {
        return boundNode.getTableSyntaxNode();
    }

    public MatchNode getTotalScore() {
        return totalScore;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            // create new instance of invoker.
            invoker = new ColumnMatchInvoker(this);
        } 
        return invoker.invoke(target, params, env);
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

    protected IMatchAlgorithmExecutor getAlgorithmExecutor() {
        return algorithmExecutor;
    }

}
