package org.openl.rules.ruleservice.instantiation;

import java.io.File;

import org.openl.rules.ruleservice.factory.WebServiceRulesEngineFactory;

public class WebServiceEngineFactoryInstantiationStrategy extends AClassInstantiationStrategy {

    private File sourceFile;
    private WebServiceRulesEngineFactory factory;
    private Object instance;
    private Class<?> serviceClass;

    public WebServiceEngineFactoryInstantiationStrategy(File sourceFile, String className, ClassLoader loader) {
        super(className, loader);
        this.sourceFile = sourceFile;
        
        init();
    }
    
    private void init() {
        
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getLoader());
        try {
            factory = new WebServiceRulesEngineFactory(sourceFile);
            serviceClass = factory.getInterfaceClass();
            instance = factory.makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    @Override
    protected Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        return instance;
    }

}
