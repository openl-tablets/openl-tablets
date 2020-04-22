package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
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
public abstract class LazyMethod extends LazyMember<IOpenMethod> {
    private final String methodName;
    private final Class<?>[] argTypes;
    private final Map<String, Object> dimensionProperties;

    private LazyMethod(IOpenMethod prebindMethod,
            Class<?>[] argTypes,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            Map<String, Object> externalParameters) {
        super(dependencyManager, classLoader, externalParameters);
        this.methodName = prebindMethod.getName();
        this.dimensionProperties = (prebindMethod instanceof ITableProperties) ? PropertiesHelper
            .getTableProperties(prebindMethod)
            .getAllDimensionalProperties() : null;
        this.argTypes = argTypes;
    }

    public static LazyMethod createLazyMethod(final IOpenMethod prebindedMethod,
            final IDependencyManager dependencyManager,
            final DeploymentDescription deployment,
            final Module module,
            final ClassLoader classLoader,
            final Map<String, Object> externalParameters) {
        Class<?>[] argTypes = new Class<?>[prebindedMethod.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = prebindedMethod.getSignature().getParameterType(i).getInstanceClass();
        }
        final LazyMethod lazyMethod = new LazyMethod(prebindedMethod,
            argTypes,
            dependencyManager,
            classLoader,
            externalParameters) {
            @Override
            public DeploymentDescription getDeployment() {
                return deployment;
            }

            @Override
            public Module getModule() {
                return module;
            }

            @Override
            public XlsLazyModuleOpenClass getXlsLazyModuleOpenClass() {
                return (XlsLazyModuleOpenClass) prebindedMethod.getDeclaringClass();
            }
        };
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyMethod));
        return lazyMethod;
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
