package org.openl.rules.runtime;

import org.openl.types.IOpenClass;

public class BaseRulesFactory implements IRulesFactory{

    private String[] includes;
    private String[] excludes;

    public String[] getExcludes() {
        return excludes;
    }
    
    public String[] getIncludes() {
        return includes;
    }
    
    public BaseRulesFactory() {
    }
    
    public BaseRulesFactory(String[] includes, String[] excludes) {
        if (includes == null) throw new IllegalArgumentException("includes arg can't be null");
        if (excludes == null) throw new IllegalArgumentException("excludes arg can't be null");
        this.includes = includes;
        this.excludes = excludes;       
    }

    public Class<?> generateInterface(String className, IOpenClass openClass, ClassLoader classLoader) throws Exception {
        boolean f1 = (includes == null) || (includes.length == 0);
        boolean f2 = (excludes == null) || (excludes.length == 0);
        if (f1 && f2) {
            return RulesFactory.generateInterface(className, openClass, classLoader);
        } else {
            return RulesFactory.generateInterface(className, openClass, classLoader, includes, excludes);
        }
    }
}
