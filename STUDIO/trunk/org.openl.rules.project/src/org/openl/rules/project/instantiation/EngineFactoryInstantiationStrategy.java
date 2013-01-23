package org.openl.rules.project.instantiation;

import org.apache.commons.lang.StringUtils;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;

import java.io.File;

/**
 * Instantiation strategy for projects with interface. Generates proxy for
 * interface by Excel file.
 * 
 * @author PUdalau
 */
public class EngineFactoryInstantiationStrategy extends SingleModuleInstantiationStrategy {

    private EngineFactory<?> engineFactory;
    
    public EngineFactoryInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
    }
    
    public EngineFactoryInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
    }
    
    @Override
    public Class<?> getServiceClass() throws ClassNotFoundException{
        // Service class for current implementation will be interface provided by user.
        //
        if (!super.isServiceClassDefined()) {
            // Load rules interface and set it to strategy.
            setServiceClass(getClassLoader().loadClass(getModule().getClassname()));
        }
        return super.getServiceClass();
    }

    @SuppressWarnings("unchecked")
    private EngineFactory<?> getEngineFactory(Class<?> clazz) {
        if(engineFactory == null){
            File sourceFile = new File(getModule().getRulesRootPath().getPath());
            
            IOpenSourceCodeModule source = new FileSourceCodeModule(sourceFile, null);
            source.setParams(prepareExternalParameters());
            
            String userHome = null;
            try {
                userHome = getModule().getProject().getProjectFolder().getAbsolutePath();
            } catch (Exception e) {
                // Ignore exception
            }
            
            if (StringUtils.isNotBlank(userHome)) {
                // Create engine factory with userHome pointing to project folder.
                // This is done for creating unique UserContext for each project, for further
                // ensuring that each project will be compiled with 
                // its custom OpenL. See {@link OpenL.getInstance(String name, IUserContext userContext).
                // Currently each project is being compiled in it`s own SimpleBundleClassLoader.
                // But it is not enough due to commented check for classloaders in 
                // {@link org.openl.conf.AUserContext.hashCode()} and {@link org.openl.conf.AUserContext.equals()}
                //
                // @author DLiauchuk
                engineFactory = new RuleEngineFactory(userHome, source, clazz);
            } else {
                engineFactory = new RuleEngineFactory(source, clazz);
            }
            
            engineFactory.setExecutionMode(isExecutionMode());
            engineFactory.setDependencyManager(getDependencyManager());
        }
        
        return engineFactory;
    }
    
    @Override
    public void reset() {
        super.reset();
        if(engineFactory != null){
            engineFactory.reset();
        }
    }
    
    @Override
    public void forcedReset() {
        super.forcedReset();
        setServiceClass(null);// it will cause reloading of service class with
                              // new classloader later
        engineFactory = null;
    }
    
    @Override
    public CompiledOpenClass compile()throws RulesInstantiationException {
        try {
            return compile(getServiceClass());
        } catch (ClassNotFoundException e) {
           throw new RulesInstantiationException("Failed to compile module", e);
        }
    }

    private CompiledOpenClass compile(Class<?> clazz) {
        EngineFactory<?> engineInstanceFactory = getEngineFactory(clazz);
        
        // Ensure that compilation will be done in strategy classLoader
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());

        try {
            return engineInstanceFactory.getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException {
        EngineFactory<?> engineInstanceFactory = getEngineFactory(rulesClass);
        
        // Ensure that instantiation will be done in strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());

        try {
            return engineInstanceFactory.makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public boolean isServiceClassDefined() {
        return true; 
    }
}
