package org.openl.rules.project.instantiation;

import java.util.Collections;
import java.util.HashMap;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.source.IOpenSourceCodeModule;

/**
 * The simplest {@link RulesInstantiationStrategy} for module that contains only Excel file.
 *
 * @author PUdalau
 */
public class ApiBasedInstantiationStrategy extends CommonRulesInstantiationStrategy {

    public ApiBasedInstantiationStrategy(Module module,
                                         IDependencyManager dependencyManager,
                                         ClassLoader classLoader,
                                         boolean executionMode) {
        super(Collections.singleton(module), executionMode, dependencyManager, classLoader);
    }

    @Override
    protected ClassLoader initClassLoader() {
        return null;
    }

    @Override
    protected IOpenSourceCodeModule createSource() {
        var externalProperties = new HashMap<String, Object>();

        var module = modules.iterator().next();

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
