package org.openl.dependency;

import org.openl.types.IOpenClass;

public interface DependencyWrapperLogicToType {
    IOpenClass apply(IOpenClass openClass, DependencyOpenClass dependencyOpenClass);
}
