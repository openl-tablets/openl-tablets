package org.openl.rules.ruleservice.management;

import java.io.File;
import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.loader.IRulesLoader;

public class RulesBasedServiceConfigurerFactory {
    public Module getModuleByName(List<Module> modules, String moduleName) {
        if (modules == null || modules.size() == 0) {
            throw new IllegalArgumentException("There are no any module to get rules based service confiurer.");
        }
        if (moduleName == null) {
            return modules.get(0);
        } else {
            Module result = null;
            for (Module module : modules) {
                if (moduleName.equals(module.getName())) {
                    return module;
                }
            }
            return result;
        }
    }

    public CommonVersion getLastVersionForDeployment(IRulesLoader loader, String deploymentName) {
        CommonVersion lastVersion = null;
        for (Deployment deployment : loader.getDeployments()) {
            if (deployment.getDeploymentName().equals(deploymentName)) {
                if (lastVersion == null || lastVersion.compareTo(deployment.getCommonVersion()) < 0) {
                    lastVersion = deployment.getCommonVersion();
                }
            }
        }
        return lastVersion;
    }

    public RulesBasedServiceConfigurer getConfigurerFromDataSource(final IRulesLoader loader,
            final String deployment,
            final String project,
            final String moduleName) {
        RulesBasedServiceConfigurer configurer = new RulesBasedServiceConfigurer() {
            @Override
            protected RulesInstantiationStrategy getRulesSource() {
                CommonVersion lastVersionForDeployment = getLastVersionForDeployment(loader, deployment);
                if (lastVersionForDeployment == null) {
                    throw new IllegalArgumentException(String.format("Wrong deployment name has been specified: \"%s\"",
                        deployment));
                }
                List<Module> modulesInSpecifiedProject = loader.resolveModulesForProject(deployment,
                    lastVersionForDeployment,
                    project);
                return getRulesInstantiationStrategy(moduleName, modulesInSpecifiedProject);
            }
        };
        return configurer;
    }

    public RulesInstantiationStrategy getRulesInstantiationStrategy(final String moduleName,
            List<Module> modulesInSpecifiedProject) {
        Module necessaryModule = getModuleByName(modulesInSpecifiedProject, moduleName);
        if (necessaryModule == null) {
            throw new IllegalArgumentException("Incorrect source folder for rules based service configurer has been specified.");
        }
        return RulesInstantiationStrategyFactory.getStrategy(necessaryModule);
    }

    public RulesBasedServiceConfigurer getConfigurerFromFilesystem(final String folder, final String moduleName) {
        RulesBasedServiceConfigurer configurer = new RulesBasedServiceConfigurer() {
            @Override
            protected RulesInstantiationStrategy getRulesSource() {
                RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
                File sourceFolder = new File(folder);
                if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
                    throw new IllegalArgumentException(String.format("Incorrect source folder for rules based service configurer has been specified: \"%s\"",
                        folder));
                }
                ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(sourceFolder);
                if (resolvingStrategy == null) {
                    throw new IllegalArgumentException(String.format("Incorrect source folder for rules based service configurer has been specified: \"%s\"." + " Can not resolve any OpenL project.",
                        folder));
                }
                ProjectDescriptor descriptor = resolvingStrategy.resolveProject(sourceFolder);
                return getRulesInstantiationStrategy(moduleName, descriptor.getModules());
            }
        };
        return configurer;
    }
}
