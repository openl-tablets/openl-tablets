package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.source.IOpenSourceCodeModule;

/**
 * The simplest {@link RulesInstantiationStrategy} for module that contains only Excel file.
 *
 * @author PUdalau
 */
public class ApiBasedInstantiationStrategy extends CommonRulesInstantiationStrategy {

    /**
     * Rules engine factory for module that contains only Excel file.
     */

    private final Module module;

    public ApiBasedInstantiationStrategy(Module module,
                                         IDependencyManager dependencyManager,
                                         ClassLoader classLoader,
                                         boolean executionMode) {
        super(executionMode, dependencyManager, classLoader);
        this.module = module;
    }

    @Override
    protected ClassLoader initClassLoader() {
        ProjectDescriptor project = module.getProject();
        return new OpenLClassLoader(project.getClassPathUrls(), Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected Collection<Module> getModules() {
        return Collections.singleton(module);
    }

    @Override
    protected IOpenSourceCodeModule createSource() {
        var externalProperties = new HashMap<String, Object>();

        if (module.getProperties() != null) {
            externalProperties.putAll(module.getProperties());
        }
        if (getExternalParameters() != null) {
            externalProperties.putAll(getExternalParameters());
        }

        var source = new ModulePathSourceCodeModule(module);
        source.setParams(externalProperties);
        return source;
    }
}
