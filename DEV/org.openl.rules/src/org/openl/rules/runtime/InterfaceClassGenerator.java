package org.openl.rules.runtime;

import org.openl.types.IOpenClass;

public interface InterfaceClassGenerator {
    Class<?> generateInterface(String className, IOpenClass openClass, ClassLoader classLoader) throws Exception;
}
