package org.openl.rules.cmatch;

import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.annotations.Executable;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

@Executable
public class ColumnMatch extends AMethod implements IMemberMetaInfo {
    private ColumnMatchBoundNode boundNode;

    private List<TableColumn> columns;
    private List<TableRow> rows;

    private Object[] returnValues;
    private MatchNode checkTree;

    private IMatchAlgorithmExecutor algorithmExecutor;

    // WEIGHT algorithm
    private MatchNode totalScore;
    private int[] columnScores;
    private Map<String, Object> properties;

    public ColumnMatch(IOpenMethodHeader header, ColumnMatchBoundNode node) {
        super(header);
        this.boundNode = node;
        properties = getSyntaxNode().getTableProperties().getAllProperties();
    }
    
    public ColumnMatchBoundNode getBoundNode() {
        return boundNode;
    }

    public void setBoundNode(ColumnMatchBoundNode boundNode) {
        this.boundNode = boundNode;
    }

    public Map<String, Object> getProperties() {
        return properties;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
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
        if (algorithmExecutor == null) {
            throw new OpenLRuntimeException(getSyntaxNode().getErrors()[0]);
        }
        
        Object result = algorithmExecutor.invoke(target, params, env, this);

        if (result == null) {
            IOpenClass type = getHeader().getType();
            if (type.getInstanceClass().isPrimitive()) {
                throw new IllegalArgumentException("Cannot return <null> for primitive type "
                        + type.getInstanceClass().getName());
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

}
