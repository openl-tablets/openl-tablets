package org.openl.dependency;

import java.util.Objects;

import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class DependencyVar extends AOpenField {

    private final DependencyType dependencyType;

    public DependencyVar(String name, IOpenClass type, DependencyType dependencyType) {
        super(name, type);
        this.dependencyType = Objects.requireNonNull(dependencyType, "dependencyType cannot be null");
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return target;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return super.getType();
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }
}
