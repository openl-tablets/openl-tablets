package org.openl.rules.ruleservice.instantiation;

public abstract class AClassInstantiationStrategy {

    private Class<?> clazz;
    private String className;
    private ClassLoader loader;

    public AClassInstantiationStrategy(Class<?> clazz) {
        this.clazz = clazz;
    }

    public AClassInstantiationStrategy(String className, ClassLoader loader) {
        this.loader = loader;
        this.className = className;
    }

    public Class<?> getServiceClass() throws ClassNotFoundException {
        if (clazz == null) {
            clazz = loader.loadClass(className);
        }
        
        return clazz;
    }

    public Object instantiate() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return instantiate(getServiceClass());
    }

    protected abstract Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException;

    protected ClassLoader getLoader() {
        return loader;
    }
}
