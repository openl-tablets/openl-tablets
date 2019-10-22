package org.openl.rules.ruleservice.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
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

    private final String dependencyName;
    private final DeploymentDescription deployment;
    private final Collection<Module> modules;
    private final boolean realCompileRequred;
    private CompiledOpenClass lazyCompiledOpenClass;
    private final boolean projectDependency;

    LazyRuleServiceDependencyLoader(DeploymentDescription deployment,
            String dependencyName,
            Collection<Module> modules,
            boolean realCompileRequred,
            boolean projectDependency) {
        this.deployment = Objects.requireNonNull(deployment, "deployment cannot null.");
        this.dependencyName = Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
        this.modules = Objects.requireNonNull(modules, "modules cannot be null");
        if (this.modules.isEmpty()) {
            throw new IllegalArgumentException("Collection of modules cannot be empty.");
        }
        this.realCompileRequred = realCompileRequred;
        this.projectDependency = projectDependency;
    }

    @Override
    public boolean isProjectDependency(String dependencyName) {
        return Objects.equals(this.dependencyName, dependencyName) && projectDependency;
    }

    @Override
    public boolean isModuleDependency(String dependencyName) {
        return Objects.equals(this.dependencyName, dependencyName) && !projectDependency;
    }

    private ClassLoader buildClassLoader(AbstractDependencyManager dependencyManager) {
        return dependencyManager.getClassLoader(modules.iterator().next().getProject());
    }

    public CompiledOpenClass compile(final String dependencyName,
            final RuleServiceDeploymentRelatedDependencyManager dependencyManager) throws OpenLCompilationException {
        if (lazyCompiledOpenClass != null) {
            return lazyCompiledOpenClass;
        }

        log.debug("Compiling lazy module for:\n" + " deployment='{}',\n" + " version='{}',\n" + " dependency='{}'",
            deployment.getName(),
            deployment.getVersion().getVersionName(),
            dependencyName);

        final ClassLoader classLoader = buildClassLoader(dependencyManager);
        RulesInstantiationStrategy rulesInstantiationStrategy = null;
        if (projectDependency) {
            rulesInstantiationStrategy = new LazyInstantiationStrategy(deployment,
                modules,
                dependencyManager,
                classLoader);
        } else {
            rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                .getStrategy(modules.iterator().next(), true, dependencyManager, classLoader);
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
                    Module module = getModuleForSourceUrl(sourceUrl, modules);
                    if (module != null) {
                        return module;
                    }
                    // Shoudn't happen
                    throw new OpenlNotCheckedException("Module is not found.");
                }

                private Module getModuleForSourceUrl(String sourceUrl, Collection<Module> modules) {
                    if (modules.size() == 1) {
                        return modules.iterator().next();
                    }
                    for (Module module : modules) {
                        String modulePath = module.getRulesRootPath().getPath();
                        try {
                            if (FilenameUtils.normalize(sourceUrl)
                                .equals(FilenameUtils.normalize(
                                    new File(modulePath).getCanonicalFile().toURI().toURL().toExternalForm()))) {
                                return module;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to build url for module '{}' with path: {}",
                                module.getName(),
                                modulePath,
                                e);
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
                dependencyManager.compilationBegin(this, modules);
                lazyCompiledOpenClass = rulesInstantiationStrategy.compile(); // Check
                // correct
                // compilation
                dependencyManager.compilationCompleted(this, !lazyCompiledOpenClass.hasErrors());
            } finally {
                if (lazyCompiledOpenClass == null) {
                    dependencyManager.compilationCompleted(this, false);
                }
            }
            if (modules.size() == 1 && realCompileRequred && lazyCompiledOpenClass != null) {
                compileAfterLazyCompile(lazyCompiledOpenClass,
                    dependencyName,
                    dependencyManager,
                    classLoader,
                    modules.iterator().next());
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
                log.debug(
                    "CompiledOpenClass for deployment='{}', version='{}', dependency='{}' has been saved in cache.",
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
    public CompiledDependency load(String dependencyName, IDependencyManager dm) throws OpenLCompilationException {
        if (this.dependencyName.equals(dependencyName)) {
            if (!(dm instanceof RuleServiceDeploymentRelatedDependencyManager)) {
                throw new IllegalStateException(String.format("This loader works only with subclasses of %s.",
                    RuleServiceDeploymentRelatedDependencyManager.class.getTypeName()));
            }

            final RuleServiceDeploymentRelatedDependencyManager dependencyManager = (RuleServiceDeploymentRelatedDependencyManager) dm;

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
        return null;
    }

    public interface LazyRuleServiceDependencyLoaderInterface {
    }
}