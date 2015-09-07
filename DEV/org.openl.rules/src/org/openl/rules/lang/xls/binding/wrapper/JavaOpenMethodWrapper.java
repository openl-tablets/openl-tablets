package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class JavaOpenMethodWrapper extends JavaOpenMethod implements IOpenMethodWrapper{
    JavaOpenMethod delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public JavaOpenMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass, JavaOpenMethod delegate) {
        super(null);
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }
    
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(xlsModuleOpenClass, this, target, params, env);
    }

    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }
    
    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    public String getName() {
        return delegate.getName();
    }

    public int getNumberOfParameters() {
        return delegate.getNumberOfParameters();
    }

    public String getParameterName(int i) {
        return delegate.getParameterName(i);
    }

    public IOpenClass getParameterType(int i) {
        return delegate.getParameterType(i);
    }

    public IOpenClass[] getParameterTypes() {
        return delegate.getParameterTypes();
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

    public String toString() {
        return delegate.toString();
    }

    
}
