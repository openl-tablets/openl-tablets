package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Map;

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
import org.openl.types.*;
import org.openl.vm.IRuntimeEnv;

public class DecisionTable2Wrapper extends DecisionTable implements IOpenMethodWrapper {
    DecisionTable delegate;
    XlsModuleOpenClass xlsModuleOpenClass;

    public DecisionTable2Wrapper(XlsModuleOpenClass xlsModuleOpenClass, DecisionTable delegate) {
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
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
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
        return delegate.getMethod();
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

    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

}
