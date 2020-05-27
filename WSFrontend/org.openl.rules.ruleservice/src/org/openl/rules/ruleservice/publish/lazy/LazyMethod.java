package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.core.RuleServiceOpenLCompilationException;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;

/**
 * Lazy method that will return real object from dependency manager. Dependency Manager is responsible for
 * loading/unloading modules.
 *
 * @author Marat Kamalov
 */
abstract class LazyMethod extends LazyMember<IOpenMethod> {
    private final String methodName;
    private final Class<?>[] argTypes;
    private final Map<String, Object> dimensionProperties;

    LazyMethod(IOpenMethod prebindMethod,
            Class<?>[] argTypes,
            RuleServiceDependencyManager dependencyManager,
            ClassLoader classLoader,
            Map<String, Object> externalParameters) {
        super(dependencyManager, classLoader, externalParameters);
        this.methodName = prebindMethod.getName();
        this.dimensionProperties = (prebindMethod instanceof ITableProperties) ? PropertiesHelper
            .getTableProperties(prebindMethod)
            .getAllDimensionalProperties() : null;
        this.argTypes = argTypes;
    }

    /**
     * Compiles method declaring the member and returns it.
     *
     * @return member in compiled module.
     */
    @Override
    public IOpenMethod getMember() {
        IOpenMethod cachedMember = getCachedMember();
        if (cachedMember != null) {
            return cachedMember;
        }
        try {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny();
            IOpenMethod openMethod = OpenClassHelper
                .findRulesMethod(compiledOpenClass.getOpenClass(), methodName, argTypes);
            if (openMethod instanceof OpenMethodDispatcher && dimensionProperties != null) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) openMethod;
                for (IOpenMethod candidate : openMethodDispatcher.getCandidates()) {
                    if (candidate instanceof ITableProperties) {
                        Map<String, Object> candidateDimensionProperties = PropertiesHelper
                            .getTableProperties(candidate)
                            .getAllDimensionalProperties();
                        if (DimensionPropertiesMethodKey.compareMethodDimensionProperties(dimensionProperties,
                            candidateDimensionProperties)) {
                            openMethod = candidate;
                            break;
                        }
                    }
                }
            }
            setCachedMember(openMethod);
            return openMethod;
        } catch (Exception e) {
            throw new RuleServiceOpenLCompilationException("Failed to load lazy method.", e);
        }
    }
}
