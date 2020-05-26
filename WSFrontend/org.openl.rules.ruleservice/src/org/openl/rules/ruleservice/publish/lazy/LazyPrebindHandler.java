package org.openl.rules.ruleservice.publish.lazy;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
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
    public IOpenMethod processPrebindMethod(IOpenMethod method) {
        final Module module = getModuleForMember(method, modules);
        LazyMethod lazyMethod = LazyMethod
            .createLazyMethod(method, dependencyManager, deployment, module, getClassLoader(), parameters);
        return LazyWrapperLogic.wrapMethod(lazyMethod, method);
    }

    @Override
    public IOpenField processPrebindField(IOpenField field) {
        final Module module = getModuleForMember(field, modules);
        final LazyField lazyField = LazyField
            .createLazyField(field, dependencyManager, deployment, module, getClassLoader(), parameters);
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
