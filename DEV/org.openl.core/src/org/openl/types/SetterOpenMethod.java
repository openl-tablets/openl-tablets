package org.openl.types;

import java.util.Objects;

import org.openl.binding.MethodUtil;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class SetterOpenMethod implements IOpenMethod, IMethodSignature {

    private final String name;

    private final IOpenField field;

    public SetterOpenMethod(IOpenField field) {
        this.field = Objects.requireNonNull(field);
        this.name = ClassUtils.setter(field.getName());
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public IMethodSignature getSignature() {
        return this;
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return field.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        field.set(target, params[0], env);
        return null;
    }

    @Override
    public int getNumberOfParameters() {
        return 1;
    }

    @Override
    public String getParameterName(int i) {
        return null;
    }

    @Override
    public IOpenClass getParameterType(int i) {
        return i == 0 ? field.getType() : null;
    }

    @Override
    public IOpenClass[] getParameterTypes() {
        return new IOpenClass[] { field.getType() };
    }
}
