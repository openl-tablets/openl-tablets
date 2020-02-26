package org.openl.rules.runtime;

import java.util.Objects;

import org.openl.types.IOpenClass;

public class InterfaceClassGeneratorImpl implements InterfaceClassGenerator {

    private String[] includes;
    private String[] excludes;

    public String[] getExcludes() {
        return excludes;
    }

    public String[] getIncludes() {
        return includes;
    }

    public InterfaceClassGeneratorImpl() {
    }

    public InterfaceClassGeneratorImpl(String[] includes, String[] excludes) {
        this.includes = Objects.requireNonNull(includes, "includes cannot be null");

        this.excludes = Objects.requireNonNull(excludes, "excludes cannot be null");

    }

    @Override
    public Class<?> generateInterface(String className,
            IOpenClass openClass,
            ClassLoader classLoader) throws Exception {
        return InterfaceGenerator.generateInterface(className, openClass, classLoader, includes, excludes);
    }
}
