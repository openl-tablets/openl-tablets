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
    
    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(this, target, params, env);
    }
    
    @Override
    public XlsModuleOpenClass getXlsModuleOpenClass() {
        return xlsModuleOpenClass;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    @Override
    public String getTableUri() {
        return delegate.getTableUri();
    }

    @Override
    public IOpenSourceCodeModule getAlgorithm() {
        return delegate.getAlgorithm();
    }
    
    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public MatchNode getCheckTree() {
        return delegate.getCheckTree();
    }

    @Override
    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    @Override
    public List<TableColumn> getColumns() {
        return delegate.getColumns();
    }

    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    @Override
    public int[] getColumnScores() {
        return delegate.getColumnScores();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public Object[] getReturnValues() {
        return delegate.getReturnValues();
    }

    @Override
    public List<TableRow> getRows() {
        return delegate.getRows();
    }

    @Override
    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    @Override
    public MatchNode getTotalScore() {
        return delegate.getTotalScore();
    }

    @Override
    public void setAlgorithmExecutor(IMatchAlgorithmExecutor algorithmExecutor) {
        delegate.setAlgorithmExecutor(algorithmExecutor);
    }

    @Override
    public void setCheckTree(MatchNode checkTree) {
        delegate.setCheckTree(checkTree);
    }

    @Override
    public void setColumns(List<TableColumn> columns) {
        delegate.setColumns(columns);
    }

    @Override
    public void setColumnScores(int[] columnScores) {
        delegate.setColumnScores(columnScores);
    }

    @Override
    public void setReturnValues(Object[] returnValues) {
        delegate.setReturnValues(returnValues);
    }

    @Override
    public void setRows(List<TableRow> rows) {
        delegate.setRows(rows);
    }

    @Override
    public void setTotalScore(MatchNode totalScore) {
        delegate.setTotalScore(totalScore);
    }

    @Override
    public IMatchAlgorithmExecutor getAlgorithmExecutor() {
        return delegate.getAlgorithmExecutor();
    }

    @Override
    public void setBoundNode(ATableBoundNode node) {
        delegate.setBoundNode(node);
    }

    @Override
    public ATableBoundNode getBoundNode() {
        return delegate.getBoundNode();
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public ITableProperties getMethodProperties() {
        return delegate.getMethodProperties();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
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
    
    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

}
