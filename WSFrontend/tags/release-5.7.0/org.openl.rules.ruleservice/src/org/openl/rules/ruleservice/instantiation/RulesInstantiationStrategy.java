package org.openl.rules.ruleservice.instantiation;

public abstract class RulesInstantiationStrategy {
    private Class<?> clazz;
    
    private String className;
    private ClassLoader loader;

    public RulesInstantiationStrategy(Class<?> clazz) {
        this.clazz = clazz;
    }

    public RulesInstantiationStrategy(String className, ClassLoader loader) {
        this.loader = loader;
        this.className = className;
    }

    /**
     * Returns <code>Class</code> object of interface or class corresponding to rules with all published methods and fields. 
     * @return interface or class
     * @throws ClassNotFoundException
     */
    public Class<?> getServiceClass() throws ClassNotFoundException {
        if (clazz == null) {
            clazz = loader.loadClass(className);
        }
        
        return clazz;
    }

    /**
     * Creates instance of class handling all rules invocations. The class will be instance of class 
     * got with {@link #getServiceClass()}.
     * 
     * @return instance of {@link #getServiceClass()} result
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public Object instantiate() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return instantiate(getServiceClass());
    }

    protected abstract Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException;

    protected ClassLoader getLoader() {
        return loader;
    }
}
