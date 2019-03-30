package org.openl.rules.runtime;

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
        if (includes == null)
            throw new IllegalArgumentException("includes arg must not be null");
        if (excludes == null)
            throw new IllegalArgumentException("excludes arg must not be null");
        this.includes = includes;
        this.excludes = excludes;
    }

    @Override
    public Class<?> generateInterface(String className,
            IOpenClass openClass,
            ClassLoader classLoader) throws Exception {
        boolean f1 = (includes == null) || (includes.length == 0);
        boolean f2 = (excludes == null) || (excludes.length == 0);
        if (f1 && f2) {
            return InterfaceGenerator.generateInterface(className, openClass, classLoader);
        } else {
            return InterfaceGenerator.generateInterface(className, openClass, classLoader, includes, excludes);
        }
    }
}
