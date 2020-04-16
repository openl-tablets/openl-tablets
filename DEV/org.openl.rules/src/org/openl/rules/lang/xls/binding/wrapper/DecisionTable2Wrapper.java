package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Map;
import java.util.Objects;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dt.DTInfo;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class DecisionTable2Wrapper extends DecisionTable implements IOpenMethodWrapper {
    static {
        WrapperLogic.validateWrapperClass(DecisionTable2Wrapper.class, DecisionTable2Wrapper.class.getSuperclass());
    }

    private final DecisionTable delegate;
    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ContextPropertiesInjector contextPropertiesInjector;
    private final IOpenClass type;
    private final IMethodSignature methodSignature;

    public DecisionTable2Wrapper(XlsModuleOpenClass xlsModuleOpenClass,
            DecisionTable delegate,
            ContextPropertiesInjector contextPropertiesInjector) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.xlsModuleOpenClass = Objects.requireNonNull(xlsModuleOpenClass, "xlsModuleOpenClass cannot be null");
        this.contextPropertiesInjector = Objects.requireNonNull(contextPropertiesInjector,
            "contextPropertiesInjector cannot be null");
        IOpenClass type = xlsModuleOpenClass.findType(delegate.getType().getName());
        this.type = type != null ? type : delegate.getType();
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
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
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
    public String getName() {
        return delegate.getName();
    }

    @Override
    public IMethodSignature getSignature() {
        return methodSignature;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public IBaseAction[] getActionRows() {
        return delegate.getActionRows();
    }

    @Override
    public IDecisionTableAlgorithm getAlgorithm() {
        return delegate.getAlgorithm();
    }

    @Override
    public int getColumns() {
        return delegate.getColumns();
    }

    @Override
    public IBaseCondition[] getConditionRows() {
        return delegate.getConditionRows();
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public int getNumberOfRules() {
        return delegate.getNumberOfRules();
    }

    @Override
    public String getRuleName(int col) {
        return delegate.getRuleName(col);
    }

    @Override
    public RuleRow getRuleRow() {
        return delegate.getRuleRow();
    }

    @Override
    public ILogicalTable getRuleTable(int ruleIndex) {
        return delegate.getRuleTable(ruleIndex);
    }

    @Override
    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    @Override
    public void setActionRows(IAction[] actionRows) {
        delegate.setActionRows(actionRows);
    }

    @Override
    public void setColumns(int columns) {
        delegate.setColumns(columns);
    }

    @Override
    public void setConditionRows(IBaseCondition[] conditionRows) {
        delegate.setConditionRows(conditionRows);
    }

    @Override
    public void setRuleRow(RuleRow ruleRow) {
        delegate.setRuleRow(ruleRow);
    }

    @Override
    public void bindTable(IBaseCondition[] conditionRows,
            IBaseAction[] actionRows,
            RuleRow ruleRow,
            OpenL openl,
            ComponentOpenClass componentOpenClass,
            IBindingContext bindingContext,
            int columns) throws Exception {
        delegate.bindTable(conditionRows, actionRows, ruleRow, openl, componentOpenClass, bindingContext, columns);
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
    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
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
    public boolean shouldFailOnMiss() {
        return delegate.shouldFailOnMiss();
    }

    @Override
    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        delegate.updateDependency(dependencies);
    }

    @Override
    public ICondition getCondition(int n) {
        return delegate.getCondition(n);
    }

    @Override
    public IAction getAction(int n) {
        return delegate.getAction(n);
    }

    @Override
    public DTInfo getDtInfo() {
        return delegate.getDtInfo();
    }

    @Override
    public void setDtInfo(DTInfo dtInfo) {
        delegate.setDtInfo(dtInfo);
    }

    @Override
    public int getNumberOfConditions() {
        return delegate.getNumberOfConditions();
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

    @Override
    public int getNumberOfActions() {
        return delegate.getNumberOfActions();
    }

    private final TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);

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
        DecisionTable2Wrapper that = (DecisionTable2Wrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
