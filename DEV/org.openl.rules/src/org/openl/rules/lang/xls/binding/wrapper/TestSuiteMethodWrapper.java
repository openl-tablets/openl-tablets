package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestMethodBoundNode;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DynamicObject;
import org.openl.vm.IRuntimeEnv;

public class TestSuiteMethodWrapper extends TestSuiteMethod implements DispatchWrapper{
    TestSuiteMethod delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public TestSuiteMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass, TestSuiteMethod delegate) {
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }
    
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return DispatcherLogic.dispatch(xlsModuleOpenClass, delegate, target, params, env);
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

    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    public IOpenClass getType() {
        return delegate.getType();
    }

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public int[] getIndices(String ids) {
        return delegate.getIndices(ids);
    }

    public TestMethodBoundNode getBoundNode() {
        return delegate.getBoundNode();
    }

    public String[] unitName() {
        return delegate.unitName();
    }

    public String getBenchmarkName() {
        return delegate.getBenchmarkName();
    }

    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    public int getNumberOfTests() {
        return delegate.getNumberOfTests();
    }

    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    public DynamicObject[] getTestObjects() {
        return delegate.getTestObjects();
    }

    public TestDescription[] getTests() {
        return delegate.getTests();
    }

    public TestDescription getTest(int numberOfTest) {
        return delegate.getTest(numberOfTest);
    }

    public String getColumnDisplayName(String columnTechnicalName) {
        return delegate.getColumnDisplayName(columnTechnicalName);
    }

    public String getColumnName(int index) {
        return delegate.getColumnName(index);
    }

    public String getColumnDisplayName(int index) {
        return delegate.getColumnDisplayName(index);
    }

    public int getColumnsCount() {
        return delegate.getColumnsCount();
    }

    public IOpenMethod getTestedMethod() {
        return delegate.getTestedMethod();
    }

    public void setBoundNode(ATableBoundNode node) {
        delegate.setBoundNode(node);
    }

    public TestUnitsResults invokeBenchmark(Object target, Object[] params, IRuntimeEnv env, long ntimes) {
        return delegate.invokeBenchmark(target, params, env, ntimes);
    }

    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    public boolean isRunmethod() {
        return delegate.isRunmethod();
    }

    public ITableProperties getMethodProperties() {
        return delegate.getMethodProperties();
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public boolean isRunmethodTestable() {
        return delegate.isRunmethodTestable();
    }

    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    public int nUnitRuns() {
        return delegate.nUnitRuns();
    }

    
}
