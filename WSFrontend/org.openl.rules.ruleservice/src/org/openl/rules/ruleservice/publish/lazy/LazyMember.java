package org.openl.rules.ruleservice.publish.lazy;

import java.util.Collections;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.MaxThreadsForCompileSemaphore;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.types.IOpenMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lazy IOpenMember that contains info about module where it was declared. When we try to do some operations with lazy
 * member it will compile module and wrap the compiled member.
 *
 * @author Marat Kamalov
 */
public abstract class LazyMember<T extends IOpenMember> {
    private final Logger log = LoggerFactory.getLogger(LazyMember.class);

    private final RuleServiceDependencyManager dependencyManager;
    private final Map<String, Object> externalParameters;

    /**
     * ClassLoader used in "lazy" compilation. It should be reused because it contains generated classes for
     * datatypes.(If we use different ClassLoaders we can get ClassCastException because generated classes for datatypes
     * have been loaded by different ClassLoaders).
     */
    private final ClassLoader classLoader;
    private volatile T cachedMember;

    public LazyMember(RuleServiceDependencyManager dependencyManager,
            ClassLoader classLoader,
            Map<String, Object> externalParameters) {
        this.dependencyManager = dependencyManager;
        this.classLoader = classLoader;
        this.externalParameters = externalParameters;
    }

    protected abstract T getMember();

    protected T getCachedMember() {
        return cachedMember;
    }

    protected void setCachedMember(T member) {
        cachedMember = member;
    }

    public void clearCachedMember() {
        cachedMember = null;
    }

    protected CompiledOpenClass getCompiledOpenClassWithThrowErrorExceptionsIfAny() throws Exception {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        if (compiledOpenClass.hasErrors()) {
            compiledOpenClass.throwErrorExceptionsIfAny();
        }
        return compiledOpenClass;
    }

    protected CompiledOpenClass getCompiledOpenClass() throws Exception {
        Module module = getModule();
        CompiledOpenClass compiledOpenClass = CompiledOpenClassCache.getInstance()
            .get(getDeployment(), module.getName());
        if (compiledOpenClass != null) {
            return compiledOpenClass;
        }

        synchronized (getXlsLazyModuleOpenClass()) {
            compiledOpenClass = CompiledOpenClassCache.getInstance().get(getDeployment(), module.getName());
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
                            .getStrategy(module, true, getDependencyManager(), getClassLoader());
                        rulesInstantiationStrategy.setServiceClass(EmptyInterface.class);// Prevent
                        Map<String, Object> parameters = ProjectExternalDependenciesHelper
                            .getExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(),
                                Collections.singleton(module));
                        rulesInstantiationStrategy.setExternalParameters(parameters);
                        compiledOpenClass1 = rulesInstantiationStrategy.compile();
                        CompiledOpenClassCache.getInstance()
                            .putToCache(getDeployment(), module.getName(), compiledOpenClass1);
                        if (log.isDebugEnabled()) {
                            log.debug(
                                "CompiledOpenClass for deploymentName='{}', deploymentVersion='{}', dependencyName='{}' was stored to cache.",
                                getDeployment().getName(),
                                getDeployment().getVersion().getVersionName(),
                                module.getName());
                        }
                        return compiledOpenClass1;
                    } catch (Exception ex) {
                        log.error("Failed to load dependency '{}'.", module.getName(), ex);
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

    public abstract XlsLazyModuleOpenClass getXlsLazyModuleOpenClass();

    /**
     * @return Module containing current member.
     */
    public abstract Module getModule();

    /**
     * @return Deployment containing current module.
     */
    public abstract DeploymentDescription getDeployment();

    /**
     * @return DependencyManager used for lazy compiling.
     */
    protected IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    /**
     * @return ClassLoader used for lazy compiling.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    public interface EmptyInterface {
    }
}
