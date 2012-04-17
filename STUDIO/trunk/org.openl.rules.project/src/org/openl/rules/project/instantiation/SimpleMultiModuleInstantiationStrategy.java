package org.openl.rules.project.instantiation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.runtime.SimpleEngineFactory;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

/**
 * The simplest way of multimodule instantiation strategy. There will be created
 * virtual module that depends on each predefined module(means virtual module
 * will have dependency for each module).
 * 
 * @author PUdalau
 * 
 */
public class SimpleMultiModuleInstantiationStrategy extends MultiModuleInstantiationStartegy {
    private final Log log = LogFactory.getLog(SimpleMultiModuleInstantiationStrategy.class);

    private SimpleEngineFactory factory;

    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        super(modules, dependencyManager);
    }

    /**
     * Construct multimodule using all modules recognized it <code>root</code>
     * folder
     * 
     * @param root Directory containing modules.
     */
    public SimpleMultiModuleInstantiationStrategy(File root) {
        this(listModules(root), null);
    }

    public SimpleMultiModuleInstantiationStrategy(List<Module> modules) {
        this(modules, null);
    }

    @Override
    public void reset() {
        super.reset();
        factory = null;
    }

    /**
     * Load modules from root folder.
     * 
     * @param root folder for all modules.
     * @return list of resolved modules.
     */
    private static List<Module> listModules(File root) {

        List<Module> modules = new ArrayList<Module>();

        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        projectResolver.setWorkspace(root.getAbsolutePath());
        List<ProjectDescriptor> projects = projectResolver.listOpenLProjects();

        for (ProjectDescriptor project : projects) {
            for (Module module : project.getModules()) {
                modules.add(module);
            }
        }

        return modules;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        } catch (Exception e) {
            throw new RulesInstantiationException("Cannot resolve interface", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException {

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(rulesClass.getClassLoader());
        try {
            return getEngineFactory().makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private SimpleEngineFactory getEngineFactory() {
    	Class<?> serviceClass = null;
    	try {
			serviceClass = getServiceClass();
		} catch (ClassNotFoundException e) {
			log.debug("Failed to get service class.", e);
			serviceClass = null;
		}
		if (factory == null
				|| (serviceClass != null && !factory.getInterfaceClass()
						.equals(serviceClass))) {
            factory = new SimpleEngineFactory(createMainModule(), AOpenLEngineFactory.DEFAULT_USER_HOME);//FIXME

            for (Module module : getModules()) {
                for (InitializingListener listener : getInitializingListeners()) {
                    listener.afterModuleLoad(module);
                }
            }
            factory.setDependencyManager(getDependencyManager());
            factory.setInterfaceClass(serviceClass);
        }

        return factory;
    }

    private IOpenSourceCodeModule createMainModule() {
        List<IDependency> dependencies = new ArrayList<IDependency>();

        for (Module module : getModules()) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("external-dependencies", dependencies);
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }
}