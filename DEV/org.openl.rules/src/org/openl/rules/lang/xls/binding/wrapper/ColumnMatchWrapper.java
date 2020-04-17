package org.openl.rules.lang.xls.binding.wrapper;

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
import org.openl.rules.lang.xls.binding.ModuleRelatedType;
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

public class ColumnMatchWrapper extends ColumnMatch implements IOpenMethodWrapper {
    static {
        WrapperLogic.validateWrapperClass(ColumnMatchWrapper.class, ColumnMatchWrapper.class.getSuperclass());
    }

    private final ColumnMatch delegate;
    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ContextPropertiesInjector contextPropertiesInjector;
    private final IOpenClass type;
    private final IMethodSignature methodSignature;

    public ColumnMatchWrapper(XlsModuleOpenClass xlsModuleOpenClass,
            ColumnMatch delegate,
            ContextPropertiesInjector contextPropertiesInjector) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.xlsModuleOpenClass = Objects.requireNonNull(xlsModuleOpenClass, "xlsModuleOpenClass cannot be null");
        this.contextPropertiesInjector = Objects.requireNonNull(contextPropertiesInjector,
            "contextPropertiesInjector cannot be null");
        if (delegate.getType() instanceof ModuleRelatedType) {
            IOpenClass type = xlsModuleOpenClass.findType(delegate.getType().getName());
            this.type = type != null ? type : delegate.getType();
        } else {
            this.type = delegate.getType();
        }
        this.methodSignature = WrapperLogic.buildMethodSignature(delegate, xlsModuleOpenClass);
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
        return this;
    }

    @Override
    public String getUri() {
        return delegate.getUri();
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
        return this;
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
        return methodSignature;
    }

    @Override
    public List<TableColumn> getColumns() {
        return delegate.getColumns();
    }

    @Override
    public IOpenClass getType() {
        return type;
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

    @Override
    public boolean isConstructor() {
        return delegate.isConstructor();
    }

    private final TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(
        this);

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

    @Override
    public ContextPropertiesInjector getContextPropertiesInjector() {
        return contextPropertiesInjector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ColumnMatchWrapper that = (ColumnMatchWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
