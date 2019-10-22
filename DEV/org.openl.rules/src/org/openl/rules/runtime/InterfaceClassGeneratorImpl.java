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
        boolean f1 = includes == null || includes.length == 0;
        boolean f2 = excludes == null || excludes.length == 0;
        if (f1 && f2) {
            return InterfaceGenerator.generateInterface(className, openClass, classLoader);
        } else {
            return InterfaceGenerator.generateInterface(className, openClass, classLoader, includes, excludes);
        }
    }
}
