package org.openl.rules.ruleservice.publish.lazy;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.publish.lazy.wrapper.LazyWrapperLogic;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LazyPrebindHandler implements IPrebindHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LazyPrebindHandler.class);
    private final Collection<Module> modules;
    private final RuleServiceDependencyManager dependencyManager;
    private final ClassLoader classLoader;
    private final Map<String, Object> parameters;
    private final DeploymentDescription deployment;

    LazyPrebindHandler(Collection<Module> modules,
            RuleServiceDependencyManager dependencyManager,
            ClassLoader classLoader,
            Map<String, Object> parameters,
            DeploymentDescription deployment) {
        this.modules = modules;
        this.dependencyManager = dependencyManager;
        this.classLoader = classLoader;
        this.parameters = parameters;
        this.deployment = deployment;
    }

    @Override
    public IOpenMethod processPrebindMethod(final IOpenMethod method) {
        final Module module = getModuleForMember(method, modules);
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }
        final LazyMethod lazyMethod = new LazyMethod(method,
            argTypes,
            dependencyManager,
            getClassLoader(),
            parameters) {
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
                return (XlsLazyModuleOpenClass) method.getDeclaringClass();
            }
        };
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyMethod));
        return LazyWrapperLogic.wrapMethod(lazyMethod, method);
    }

    @Override
    public IOpenField processPrebindField(final IOpenField field) {
        final Module module = getModuleForMember(field, modules);
        final LazyField lazyField = new LazyField(field.getName(), dependencyManager, getClassLoader(), parameters) {
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
                return (XlsLazyModuleOpenClass) field.getDeclaringClass();
            }
        };
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyField));
        return LazyWrapperLogic.wrapField(lazyField, field);
    }

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
