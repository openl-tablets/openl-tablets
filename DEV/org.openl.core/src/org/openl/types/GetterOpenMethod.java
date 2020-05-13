package org.openl.types;

import java.util.Objects;

import org.openl.binding.MethodUtil;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class GetterOpenMethod implements IOpenMethod, IMethodSignature {

    private final String name;

    private final IOpenField field;

    public GetterOpenMethod(IOpenField field) {
        this.field = Objects.requireNonNull(field);
        this.name = ClassUtils.getter(field.getName());
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
        return field.getType();
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
        return field.get(target, env);
    }

    @Override
    public int getNumberOfParameters() {
        return 0;
    }

    @Override
    public String getParameterName(int i) {
        return null;
    }

    @Override
    public IOpenClass getParameterType(int i) {
        return null;
    }

    @Override
    public IOpenClass[] getParameterTypes() {
        return IOpenClass.EMPTY;
    }
}
