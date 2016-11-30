package org.openl.rules.lang.xls.binding.wrapper;

import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class AlgorithmWrapper extends Algorithm implements IOpenMethodWrapper{
    Algorithm delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public AlgorithmWrapper(XlsModuleOpenClass xlsModuleOpenClass, Algorithm delegate) {
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

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    public String getTableUri() {
        return delegate.getTableUri();
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

    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    public void setAlgorithmSteps(List<RuntimeOperation> algorithmSteps) {
        delegate.setAlgorithmSteps(algorithmSteps);
    }

    public void setLabels(Map<String, RuntimeOperation> labels) {
        delegate.setLabels(labels);
    }

    public void setThisClass(IOpenClass thisClass) {
        delegate.setThisClass(thisClass);
    }

    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
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
