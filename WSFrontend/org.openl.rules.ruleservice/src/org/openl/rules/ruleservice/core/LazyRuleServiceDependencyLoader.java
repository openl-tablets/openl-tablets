package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.core.RuleServiceDeploymentRelatedDependencyManager.DependencyCompilationType;
import org.openl.rules.ruleservice.publish.lazy.CompiledOpenClassCache;
import org.openl.rules.ruleservice.publish.lazy.LazyBinderInvocationHandler;
import org.openl.rules.ruleservice.publish.lazy.LazyCompiledOpenClass;
import org.openl.rules.ruleservice.publish.lazy.LazyField;
import org.openl.rules.ruleservice.publish.lazy.LazyInstantiationStrategy;
import org.openl.rules.ruleservice.publish.lazy.LazyMember.EmptyInterface;
import org.openl.rules.ruleservice.publish.lazy.LazyMethod;
import org.openl.rules.ruleservice.publish.lazy.ModuleUtils;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LazyRuleServiceDependencyLoader implements IDependencyLoader {

    private final Logger log = LoggerFactory.getLogger(LazyRuleServiceDependencyLoader.class);

    private final RuleServiceDeploymentRelatedDependencyManager dependencyManager;
    private final String dependencyName;
    private final boolean realCompileRequired;
    private CompiledOpenClass lazyCompiledOpenClass;
    private final DeploymentDescription deployment;
    private final ProjectDescriptor project;
    private final Module module;

    public LazyRuleServiceDependencyLoader(DeploymentDescription deployment,
            ProjectDescriptor project,
            Module module,
            boolean realCompileRequired,
            RuleServiceDeploymentRelatedDependencyManager dependencyManager) {
        this.deployment = Objects.requireNonNull(deployment, "deployment cannot null.");
        this.project = Objects.requireNonNull(project, "project cannot be null");
        this.module = module;
        this.realCompileRequired = realCompileRequired;
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.dependencyName = SimpleDependencyLoader.buildDependencyName(project, module);
    }

    @Override
    public boolean isCompiled() {
        return lazyCompiledOpenClass != null;
    }

    private Collection<Module> getModules() {
        return module != null ? Collections.singleton(module) : project.getModules();
    }

    @Override
    public boolean isProject() {
        return module == null;
    }

    @Override
    public ProjectDescriptor getProject() {
        return project;
    }

    public String getDependencyName() {
        return dependencyName;
    }

    private ClassLoader buildClassLoader(AbstractDependencyManager dependencyManager) {
        return dependencyManager.getClassLoader(getProject());
    }

    public CompiledOpenClass compile(final String dependencyName,
            final RuleServiceDeploymentRelatedDependencyManager dependencyManager) throws OpenLCompilationException {
        if (lazyCompiledOpenClass != null) {
            return lazyCompiledOpenClass;
        }

        log.debug("Compiling lazy dependency: deployment='{}', version='{}', name='{}'.",
            deployment.getName(),
            deployment.getVersion().getVersionName(),
            dependencyName);

        final ClassLoader classLoader = buildClassLoader(dependencyManager);
        RulesInstantiationStrategy rulesInstantiationStrategy;
        Collection<Module> modules = getModules();
        if (isProject()) {
            if (modules.isEmpty()) {
                throw new IllegalStateException("Expected at least one module in the project.");
            }
            rulesInstantiationStrategy = new LazyInstantiationStrategy(deployment,
                modules,
                dependencyManager,
                classLoader);
        } else {
            rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                .getStrategy(module, true, dependencyManager, classLoader);
        }
        rulesInstantiationStrategy.setServiceClass(LazyRuleServiceDependencyLoaderInterface.class);// Prevent
        // generation
        // interface
        // and
        // Virtual
        // module
        // duplicate
        // (instantiate
        // method).
        // Improve
        // performance.
        final Map<String, Object> parameters = ProjectExternalDependenciesHelper
            .getExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(), modules);
        rulesInstantiationStrategy.setExternalParameters(parameters);
        IPrebindHandler prebindHandler = LazyBinderInvocationHandler.getPrebindHandler();
        try {
            LazyBinderInvocationHandler.setPrebindHandler(new IPrebindHandler() {

                @Override
                public IOpenMethod processMethodAdded(IOpenMethod method, XlsLazyModuleOpenClass moduleOpenClass) {
                    final Module declaringModule = ModuleUtils.getModuleForMember(method, modules);
                    Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
                    for (int i = 0; i < argTypes.length; i++) {
                        argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
                    }

                    return LazyMethod.getLazyMethod(moduleOpenClass,
                        deployment,
                        declaringModule,
                        argTypes,
                        method,
                        dependencyManager,
                        classLoader,
                        true,
                        parameters);
                }

                @Override
                public IOpenField processFieldAdded(IOpenField field, XlsLazyModuleOpenClass moduleOpenClass) {
                    Module declaringModule = ModuleUtils.getModuleForMember(field, modules);
                    return LazyField.getLazyField(moduleOpenClass,
                        deployment,
                        declaringModule,
                        field,
                        dependencyManager,
                        classLoader,
                        true,
                        parameters);
                }
            });
            try {
                dependencyManager.compilationBegin(this);
                lazyCompiledOpenClass = rulesInstantiationStrategy.compile();
                if (!isProject() && realCompileRequired) {
                    compileAfterLazyCompile(lazyCompiledOpenClass,
                        dependencyName,
                        dependencyManager,
                        classLoader,
                        modules.iterator().next());
                }
                dependencyManager.compilationCompleted(this,
                    realCompileRequired ? DependencyCompilationType.UNLOADABLE : DependencyCompilationType.LAZY,
                    !lazyCompiledOpenClass.hasErrors());
            } finally {
                if (lazyCompiledOpenClass == null) {
                    dependencyManager.compilationCompleted(this,
                        realCompileRequired ? DependencyCompilationType.UNLOADABLE : DependencyCompilationType.LAZY,
                        false);
                }
            }
            return lazyCompiledOpenClass;
        } catch (Exception ex) {
            throw new OpenLCompilationException(String.format("Failed to load dependency '%s'.", dependencyName), ex);
        } finally {
            LazyBinderInvocationHandler.setPrebindHandler(prebindHandler);
        }
    }

    private void compileAfterLazyCompile(CompiledOpenClass lazyCompiledOpenClass,
            final String dependencyName,
            final RuleServiceDeploymentRelatedDependencyManager dependencyManager,
            final ClassLoader classLoader,
            final Module module) throws OpenLCompilationException {
        synchronized (lazyCompiledOpenClass) {
            CompiledOpenClass compiledOpenClass = CompiledOpenClassCache.getInstance().get(deployment, dependencyName);
            if (compiledOpenClass != null) {
                return;
            }
            IPrebindHandler prebindHandler = LazyBinderInvocationHandler.getPrebindHandler();
            try {
                LazyBinderInvocationHandler.removePrebindHandler();
                RulesInstantiationStrategy rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                    .getStrategy(module, true, dependencyManager, classLoader);
                rulesInstantiationStrategy.setServiceClass(EmptyInterface.class);
                Map<String, Object> parameters = ProjectExternalDependenciesHelper
                    .getExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(),
                        new ArrayList<Module>() {
                            private static final long serialVersionUID = 1L;

                            {
                                add(module);
                            }
                        });
                rulesInstantiationStrategy.setExternalParameters(parameters);
                compiledOpenClass = rulesInstantiationStrategy.compile();
                CompiledOpenClassCache.getInstance().putToCache(deployment, dependencyName, compiledOpenClass);
                log.debug("Compiled lazy dependency (deployment='{}', version='{}', name='{}') is saved in cache.",
                    deployment.getName(),
                    deployment.getVersion().getVersionName(),
                    dependencyName);
            } catch (Exception ex) {
                throw new OpenLCompilationException(String.format("Failed to load dependency '%s'.", dependencyName),
                    ex);
            } finally {
                LazyBinderInvocationHandler.setPrebindHandler(prebindHandler);
            }
        }
    }

    private boolean isCompiledBefore = false;
    private CompiledDependency lazyCompiledDependency = null;

    @Override
    public final CompiledDependency getCompiledDependency() throws OpenLCompilationException {
        if (!isCompiledBefore) {
            compile(dependencyName, dependencyManager);
            isCompiledBefore = true;
        }
        if (lazyCompiledDependency == null) {
            CompiledOpenClass compiledOpenClass = new LazyCompiledOpenClass(dependencyManager,
                this,
                new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, dependencyName, null)));
            lazyCompiledDependency = new CompiledDependency(dependencyName, compiledOpenClass);
        }
        return lazyCompiledDependency;
    }

    @Override
    public void reset() {
        // Nothing to reset
    }

    public interface LazyRuleServiceDependencyLoaderInterface {
    }
}