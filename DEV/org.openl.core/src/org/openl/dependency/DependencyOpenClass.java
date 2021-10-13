package org.openl.dependency;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.OpenClassDelegator;

public class DependencyOpenClass extends OpenClassDelegator {

    public static DependencyWrapperLogicToMethod dependencyWrapperLogicToMethod;

    private final Map<IOpenMethod, IOpenMethod> dependencyLogicAppliedToMethodMap = new HashMap<>();

    private final String dependencyName;

    public DependencyOpenClass(String dependencyName, IOpenClass delegate) {
        super(delegate);
        this.dependencyName = Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
    }

    @Override
    public String getName() {
        return dependencyName;
    }

    private IOpenMethod applyDependencyLogicToMethod(IOpenMethod openMethod) {
        if (dependencyWrapperLogicToMethod != null && openMethod != null) {
            IOpenMethod m = dependencyLogicAppliedToMethodMap.get(openMethod);
            if (m == null) {
                m = dependencyWrapperLogicToMethod.apply(openMethod, this);
                dependencyLogicAppliedToMethodMap.put(openMethod, m);
            }
            return m;
        }
        return openMethod;
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        return super.getTypes();
    }

    @Override
    public IOpenClass findType(String name) {
        return super.findType(name);
    }

    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        IOpenMethod method = super.getMethod(name, classes);
        if (method == null) {
            return null;
        }
        return applyDependencyLogicToMethod(method);
    }

    @Override
    public Collection<IOpenMethod> getMethods() {
        return super.getMethods().stream().map(this::applyDependencyLogicToMethod).collect(Collectors.toList());
    }
}
