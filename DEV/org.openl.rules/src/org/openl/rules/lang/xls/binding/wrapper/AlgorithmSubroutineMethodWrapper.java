package org.openl.rules.lang.xls.binding.wrapper;

import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class AlgorithmSubroutineMethodWrapper extends AlgorithmSubroutineMethod implements IOpenMethodWrapper {
    AlgorithmSubroutineMethod delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public AlgorithmSubroutineMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass, AlgorithmSubroutineMethod delegate) {
        super(null);
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
    public IOpenMethod getDelegate() {
        return delegate;
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
    public String getUri() {
        return delegate.getUri();
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
    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    @Override
    public IGridRegion getGridRegion() {
        return delegate.getGridRegion();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    @Override
    public void setAlgorithmSteps(List<RuntimeOperation> operations) {
        delegate.setAlgorithmSteps(operations);
    }

    @Override
    public List<RuntimeOperation> getAlgorithmSteps() {
        return delegate.getAlgorithmSteps();
    }

    @Override
    public void setLabels(Map<String, RuntimeOperation> localLabelsRegister) {
        delegate.setLabels(localLabelsRegister);
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
    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
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
    public ITableProperties getMethodProperties() {
        return delegate.getMethodProperties();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }
    
    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }
    
    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
    }

    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache();

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

}
