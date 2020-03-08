package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Map;
import java.util.Objects;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class CompositeMethodWrapper extends CompositeMethod implements IOpenMethodWrapper {
    static {
        WrapperLogic.validateWrapperClass(CompositeMethodWrapper.class, CompositeMethodWrapper.class.getSuperclass());
    }

    private CompositeMethod delegate;
    private XlsModuleOpenClass xlsModuleOpenClass;
    private ContextPropertiesInjector contextPropertiesInjector;
    private IOpenClass type;
    private IMethodSignature methodSignature;

    public CompositeMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass,
            CompositeMethod delegate,
            ContextPropertiesInjector contextPropertiesInjector) {
        super(null, null);
        this.delegate = Objects.requireNonNull(delegate, "delegate can not be null");
        this.xlsModuleOpenClass = Objects.requireNonNull(xlsModuleOpenClass, "xlsModuleOpenClass can not be null");
        this.contextPropertiesInjector = Objects.requireNonNull(contextPropertiesInjector,
            "contextPropertiesInjector can not be null");
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
    public IOpenClass getBodyType() {
        return delegate.getBodyType();
    }

    @Override
    public IOpenMethodHeader getHeader() {
        return this;
    }

    @Override
    public IBoundMethodNode getMethodBodyBoundNode() {
        return delegate.getMethodBodyBoundNode();
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public boolean isInvokable() {
        return delegate.isInvokable();
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
    public void removeDebugInformation() {
        delegate.removeDebugInformation();
    }

    @Override
    public void setMethodBodyBoundNode(IBoundMethodNode node) {
        delegate.setMethodBodyBoundNode(node);
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        delegate.updateDependency(dependencies);
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
    public ISyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    @Override
    public String getSourceUrl() {
        return delegate.getSourceUrl();
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

    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);

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
        CompositeMethodWrapper that = (CompositeMethodWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
