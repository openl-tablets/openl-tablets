package org.openl.dependency;

import org.openl.types.IOpenMethod;

public interface DependencyWrapperLogicToMethod {

    IOpenMethod apply(IOpenMethod openMethod, DependencyOpenClass dependencyOpenClass);
}
