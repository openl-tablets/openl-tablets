package org.openl.rules.project.instantiation;

import java.io.File;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.util.StringUtils;

/**
 * Instantiation strategy for projects with interface. Generates proxy for
 * interface by Excel file.
 * 
 * @author PUdalau
 */
public class SimpleEngineFactoryInstantiationStrategy extends SingleModuleInstantiationStrategy {

    private RulesEngineFactory<?> engineFactory;
    
    public SimpleEngineFactoryInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
    }
    
    public SimpleEngineFactoryInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<Object> getServiceClass() throws ClassNotFoundException{
        // Service class for current implementation will be interface provided by user.
        //
        if (!super.isServiceClassDefined()) {
            // Load rules interface and set it to strategy.
            setServiceClass(getClassLoader().loadClass(getModule().getClassname()));
        }
        return (Class<Object>)super.getServiceClass();
    }
    
    @SuppressWarnings("unchecked")
    private RulesEngineFactory<?> getEngineFactory(Class<?> clazz) {
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
                engineFactory = new RulesEngineFactory<Object>(source, userHome, (Class<Object>)clazz);
            } else {
                engineFactory = new RulesEngineFactory<Object>(source, (Class<Object>)clazz);
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
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException {
        RulesEngineFactory<?> engineInstanceFactory = getEngineFactory(rulesClass);
        
        // Ensure that instantiation will be done in strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());

        try {
            return engineInstanceFactory.newInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public boolean isServiceClassDefined() {
        return true; 
    }

	@Override
	protected RulesEngineFactory<?> getEngineFactory() throws RulesInstantiationException {
		try {
			return this.getEngineFactory(getServiceClass());
		} catch (ClassNotFoundException e) {
			throw new RulesInstantiationException(e);
		}
	}
}
