package org.openl.rules.lang.xls.binding.wrapper;

import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.TableColumn;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class ColumnMatchWrapper extends ColumnMatch implements IOpenMethodWrapper{
    ColumnMatch delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public ColumnMatchWrapper(XlsModuleOpenClass xlsModuleOpenClass, ColumnMatch delegate) {
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }
    
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(xlsModuleOpenClass, this, target, params, env);
    }

    public String toString() {
        return delegate.toString();
    }

    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    public String getTableUri() {
        return delegate.getTableUri();
    }

    public IOpenSourceCodeModule getAlgorithm() {
        return delegate.getAlgorithm();
    }
    
    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    public String getName() {
        return delegate.getName();
    }

    public MatchNode getCheckTree() {
        return delegate.getCheckTree();
    }

    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    public List<TableColumn> getColumns() {
        return delegate.getColumns();
    }

    public IOpenClass getType() {
        return delegate.getType();
    }

    public int[] getColumnScores() {
        return delegate.getColumnScores();
    }

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    public Object[] getReturnValues() {
        return delegate.getReturnValues();
    }

    public List<TableRow> getRows() {
        return delegate.getRows();
    }

    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    public MatchNode getTotalScore() {
        return delegate.getTotalScore();
    }

    public void setAlgorithmExecutor(IMatchAlgorithmExecutor algorithmExecutor) {
        delegate.setAlgorithmExecutor(algorithmExecutor);
    }

    public void setCheckTree(MatchNode checkTree) {
        delegate.setCheckTree(checkTree);
    }

    public void setColumns(List<TableColumn> columns) {
        delegate.setColumns(columns);
    }

    public void setColumnScores(int[] columnScores) {
        delegate.setColumnScores(columnScores);
    }

    public void setReturnValues(Object[] returnValues) {
        delegate.setReturnValues(returnValues);
    }

    public void setRows(List<TableRow> rows) {
        delegate.setRows(rows);
    }

    public void setTotalScore(MatchNode totalScore) {
        delegate.setTotalScore(totalScore);
    }

    public IMatchAlgorithmExecutor getAlgorithmExecutor() {
        return delegate.getAlgorithmExecutor();
    }

    public void setBoundNode(ATableBoundNode node) {
        delegate.setBoundNode(node);
    }

    public ATableBoundNode getBoundNode() {
        return delegate.getBoundNode();
    }

    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    public ITableProperties getMethodProperties() {
        return delegate.getMethodProperties();
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }
    
    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }
    
    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
    }

}
