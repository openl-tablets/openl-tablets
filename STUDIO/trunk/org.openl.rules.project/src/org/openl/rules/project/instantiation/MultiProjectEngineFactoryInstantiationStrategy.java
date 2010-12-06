package org.openl.rules.project.instantiation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.dependencies.RulesProjectDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.runtime.RulesFileDependencyLoader;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

public class MultiProjectEngineFactoryInstantiationStrategy extends RulesInstantiationStrategy {
    private static final Log LOG = LogFactory.getLog(MultiProjectEngineFactoryInstantiationStrategy.class);

    private File root;
    private MultiProjectEngineFactory factory;

    public MultiProjectEngineFactoryInstantiationStrategy(File root) {
        super(null, true, null);        
        
        this.root = root;
        getEngineFactory();
    }

    private MultiProjectEngineFactory getEngineFactory() {
        if (factory == null) {
            factory = new MultiProjectEngineFactory(root);
        }
        
        return factory;
    }

    @Override
    protected void forcedReset() {
    }

    @Override
    protected ClassLoader getClassLoader() {
        return factory.getDefaultUserClassLoader();
    }

    @Override
    public Class<?> getServiceClass() {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        }catch (Exception e) {
            LOG.warn("Cannot resolve interface", e);
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public void addInitializingListener(InitializingListener listener) {
        getEngineFactory().addInitializingListener(listener);
    }
    
    public void removeInitializingListener(InitializingListener listener) {
        getEngineFactory().removeInitializingListener(listener);
    }
    
    @Override
    protected CompiledOpenClass compile(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            IOpenClass openClass = factory.getOpenClass(); 
            return new CompiledOpenClass(openClass, new ArrayList<OpenLMessage>(), new SyntaxNodeException[0], new SyntaxNodeException[0]);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected Object instantiate(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        try {
            return factory.makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}