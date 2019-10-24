package org.openl.rules.ruleservice.core;

import java.io.File;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.*;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.core.RuleServiceDeploymentRelatedDependencyManager.DependencyCompilationType;
import org.openl.rules.ruleservice.publish.lazy.*;
import org.openl.rules.ruleservice.publish.lazy.LazyMember.EmptyInterface;
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
    private final boolean realCompileRequred;
    private CompiledOpenClass lazyCompiledOpenClass;
    private final DeploymentDescription deployment;
    private final ProjectDescriptor project;
    private final Module module;

    public LazyRuleServiceDependencyLoader(DeploymentDescription deployment,
            ProjectDescriptor project,
            Module module,
            boolean realCompileRequred,
            RuleServiceDeploymentRelatedDependencyManager dependencyManager) {
        this.deployment = Objects.requireNonNull(deployment, "deployment cannot null.");
        this.project = Objects.requireNonNull(project, "project cannot be null");
        this.module = module;
        this.realCompileRequred = realCompileRequred;
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.dependencyName = SimpleDependencyLoader.buildDependencyName(project, module);
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
        RulesInstantiationStrategy rulesInstantiationStrategy = null;
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
                Module getModuleForMember(IOpenMember member) {
                    String sourceUrl = member.getDeclaringClass().getMetaInfo().getSourceUrl();
                    Module m = getModuleForSourceUrl(sourceUrl, modules);
                    if (m != null) {
                        return m;
                    }
                    throw new OpenlNotCheckedException("Module is not found. This shoud not happen.");
                }

                private Module getModuleForSourceUrl(String sourceUrl, Collection<Module> modules) {
                    if (modules.size() == 1) {
                        return modules.iterator().next();
                    }
                    for (Module m : modules) {
                        String modulePath = m.getRulesRootPath().getPath();
                        try {
                            if (FilenameUtils.normalize(sourceUrl)
                                .equals(FilenameUtils.normalize(
                                    new File(modulePath).getCanonicalFile().toURI().toURL().toExternalForm()))) {
                                return m;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to build url for module '{}' with path {}.", m.getName(), modulePath, e);
                        }
                    }
                    return null;
                }

                @Override
                public IOpenMethod processMethodAdded(IOpenMethod method, XlsLazyModuleOpenClass moduleOpenClass) {
                    final Module declaringModule = getModuleForMember(method);
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
                    Module declaringModule = getModuleForMember(field);
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
                if (!isProject() && realCompileRequred) {
                    compileAfterLazyCompile(lazyCompiledOpenClass,
                        dependencyName,
                        dependencyManager,
                        classLoader,
                        modules.iterator().next());
                }
                dependencyManager.compilationCompleted(this,
                    realCompileRequred ? DependencyCompilationType.UNLOADABLE : DependencyCompilationType.LAZY,
                    !lazyCompiledOpenClass.hasErrors());
            } finally {
                if (lazyCompiledOpenClass == null) {
                    dependencyManager.compilationCompleted(this,
                        realCompileRequred ? DependencyCompilationType.UNLOADABLE : DependencyCompilationType.LAZY,
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