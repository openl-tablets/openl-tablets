package org.openl.rules.lang.xls.binding.wrapper.base;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.binding.BindingDependencies;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.TableColumn;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public abstract class AbstractColumnMatchWrapper extends ColumnMatch {
    static {
        WrapperValidation.validateWrapperClass(AbstractColumnMatchWrapper.class,
            AbstractColumnMatchWrapper.class.getSuperclass());
    }

    protected final ColumnMatch delegate;

    public AbstractColumnMatchWrapper(ColumnMatch delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public IOpenSourceCodeModule getAlgorithm() {
        return delegate.getAlgorithm();
    }

    @Override
    public MatchNode getCheckTree() {
        return delegate.getCheckTree();
    }

    @Override
    public List<TableColumn> getColumns() {
        return delegate.getColumns();
    }

    @Override
    public int[] getColumnScores() {
        return delegate.getColumnScores();
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
    public String getUri() {
        return delegate.getUri();
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return delegate.invoke(target, params, env);
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
    public boolean isConstructor() {
        return delegate.isConstructor();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }

    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
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
        return this;
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractColumnMatchWrapper that = (AbstractColumnMatchWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}