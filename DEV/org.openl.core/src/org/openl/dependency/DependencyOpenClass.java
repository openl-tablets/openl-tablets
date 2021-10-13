package org.openl.dependency;

import java.util.Collection;
import java.util.stream.Collectors;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.OpenClassDelegator;

public class DependencyOpenClass extends OpenClassDelegator {

    public static DependencyWrapperLogic dependencyWrapperLogic;

    public DependencyOpenClass(IOpenClass delegate) {
        super(delegate);
    }

    private IOpenMethod applyDependencyLogic(IOpenMethod openMethod) {
        if (dependencyWrapperLogic != null) {
            return dependencyWrapperLogic.applyDependencyLogic(openMethod);
        }
        return openMethod;
    }

    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        IOpenMethod method = super.getMethod(name, classes);
        if (method == null) {
            return null;
        }
        return applyDependencyLogic(method);
    }

    @Override
    public Collection<IOpenMethod> getMethods() {
        return super.getMethods().stream().map(this::applyDependencyLogic).collect(Collectors.toList());
    }
}
