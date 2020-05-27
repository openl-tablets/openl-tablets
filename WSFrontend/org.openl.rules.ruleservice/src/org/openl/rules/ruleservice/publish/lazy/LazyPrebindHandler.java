package org.openl.rules.ruleservice.publish.lazy;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.MaxThreadsForCompileSemaphore;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.core.RuleServiceOpenLCompilationException;
import org.openl.rules.ruleservice.publish.lazy.wrapper.LazyWrapperLogic;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LazyPrebindHandler implements IPrebindHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LazyPrebindHandler.class);
    private final Collection<Module> modules;
    private final RuleServiceDependencyManager dependencyManager;
    private final ClassLoader classLoader;
    private final DeploymentDescription deployment;

    LazyPrebindHandler(Collection<Module> modules,
            RuleServiceDependencyManager dependencyManager,
            ClassLoader classLoader,
            DeploymentDescription deployment) {
        this.modules = modules;
        this.dependencyManager = dependencyManager;
        this.classLoader = classLoader;
        this.deployment = deployment;
    }

    @Override
    public IOpenMethod processPrebindMethod(final IOpenMethod method) {
        final Module module = getModuleForMember(method, modules);
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }
        final Map<String, Object> dimensionProperties = (method instanceof ITableProperties) ? PropertiesHelper
            .getTableProperties(method)
            .getAllDimensionalProperties() : null;
        final LazyMember<IOpenMethod> lazyMethod = new LazyMember<IOpenMethod>() {

            protected IOpenMethod initMember() {
                IOpenMethod openMethod;
                try {
                    CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny(method,
                        module);
                    openMethod = OpenClassHelper
                        .findRulesMethod(compiledOpenClass.getOpenClass(), method.getName(), argTypes);
                    if (openMethod instanceof OpenMethodDispatcher && dimensionProperties != null) {
                        openMethod = findCandidateMethod((OpenMethodDispatcher) openMethod, dimensionProperties);
                    }
                } catch (Exception e) {
                    throw new RuleServiceOpenLCompilationException("Failed to load lazy method.", e);
                }
                return openMethod;
            }

        };
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyMethod));
        return LazyWrapperLogic.wrapMethod(lazyMethod, method);
    }

    private IOpenMethod findCandidateMethod(OpenMethodDispatcher openMethod, Map<String, Object> dimensionProperties) {
        for (IOpenMethod candidate : openMethod.getCandidates()) {
            if (candidate instanceof ITableProperties) {
                Map<String, Object> candidateDimensionProperties = PropertiesHelper.getTableProperties(candidate)
                    .getAllDimensionalProperties();
                if (DimensionPropertiesMethodKey.compareMethodDimensionProperties(dimensionProperties,
                    candidateDimensionProperties)) {
                    return candidate;
                }
            }
        }
        return openMethod;
    }

    @Override
    public IOpenField processPrebindField(final IOpenField field) {
        final Module module = getModuleForMember(field, modules);
        final LazyMember<IOpenField> lazyField = new LazyMember<IOpenField>() {

            protected IOpenField initMember() {
                try {
                    CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny(field,
                        module);
                    return compiledOpenClass.getOpenClass().getField(field.getName());
                } catch (Exception e) {
                    throw new RuleServiceOpenLCompilationException("Failed to load a lazy field.", e);
                }
            }

        };
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyField));
        return LazyWrapperLogic.wrapField(lazyField, field);
    }

    private CompiledOpenClass getCompiledOpenClassWithThrowErrorExceptionsIfAny(IOpenMember sync,
            Module module) throws Exception {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass(sync, module);
        if (compiledOpenClass.hasErrors()) {
            compiledOpenClass.throwErrorExceptionsIfAny();
        }
        return compiledOpenClass;
    }

    private CompiledOpenClass getCompiledOpenClass(IOpenMember sync, Module module) throws Exception {
        CompiledOpenClass compiledOpenClass = CompiledOpenClassCache.getInstance().get(deployment, module.getName());
        if (compiledOpenClass != null) {
            return compiledOpenClass;
        }

        synchronized (sync.getDeclaringClass()) {
            compiledOpenClass = CompiledOpenClassCache.getInstance().get(deployment, module.getName());
            if (compiledOpenClass != null) {
                return compiledOpenClass;
            }
            try {
                return MaxThreadsForCompileSemaphore.getInstance().run(() -> {
                    CompiledOpenClass compiledOpenClass1 = null;
                    IPrebindHandler prebindHandler = LazyBinderMethodHandler.getPrebindHandler();
                    try {
                        LazyBinderMethodHandler.removePrebindHandler();
                        RulesInstantiationStrategy rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                            .getStrategy(module, true, dependencyManager, getClassLoader());
                        rulesInstantiationStrategy.setServiceClass(EmptyInterface.class);// Prevent
                        Map<String, Object> parameters = ProjectExternalDependenciesHelper
                            .getExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(),
                                Collections.singleton(module));
                        rulesInstantiationStrategy.setExternalParameters(parameters);
                        compiledOpenClass1 = rulesInstantiationStrategy.compile();
                        CompiledOpenClassCache.getInstance()
                            .putToCache(deployment, module.getName(), compiledOpenClass1);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(
                                "CompiledOpenClass for deploymentName='{}', deploymentVersion='{}', dependencyName='{}' was stored to cache.",
                                deployment.getName(),
                                deployment.getVersion().getVersionName(),
                                module.getName());
                        }
                        return compiledOpenClass1;
                    } catch (Exception ex) {
                        LOG.error("Failed to load dependency '{}'.", module.getName(), ex);
                        return compiledOpenClass1;
                    } finally {
                        LazyBinderMethodHandler.setPrebindHandler(prebindHandler);
                    }
                });
            } catch (OpenLCompilationException e) {
                throw e;
            } catch (InterruptedException e) {
                throw new OpenLCompilationException("Interrupted exception.", e);
            } catch (Exception e) {
                throw new OpenLCompilationException("Failed to compile.", e);
            }
        }
    }

    /**
     * ClassLoader used in "lazy" compilation. It should be reused because it contains generated classes for
     * datatypes.(If we use different ClassLoaders we can get ClassCastException because generated classes for datatypes
     * have been loaded by different ClassLoaders).
     */
    private ClassLoader getClassLoader() {
        return classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
    }

    private static Module getModuleForMember(IOpenMember member, Collection<Module> modules) {
        String sourceUrl = member.getDeclaringClass().getMetaInfo().getSourceUrl();
        Module module = getModuleForSourceUrl(sourceUrl, modules);
        if (module != null) {
            return module;
        }
        throw new OpenlNotCheckedException("Module is not found. This shoud not happen.");
    }

    private static Module getModuleForSourceUrl(String sourceUrl, Collection<Module> modules) {
        if (modules.size() == 1) {
            return modules.iterator().next();
        }
        for (Module module : modules) {
            String modulePath = module.getRulesRootPath().getPath();
            try {
                if (Paths.get(sourceUrl)
                    .normalize()
                    .equals(Paths.get(new File(modulePath).getCanonicalFile().toURI().toURL().toExternalForm())
                        .normalize())) {
                    return module;
                }
            } catch (Exception e) {
                LOG.warn("Failed to build url for module '{}' with path: {}", module.getName(), modulePath, e);
            }
        }
        return null;
    }
}
