package org.openl.rules.ruleservice.conf;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.springframework.beans.factory.FactoryBean;

public abstract class AbstractRulesBasedServiceConfigurerFactoryBean implements FactoryBean<RulesBasedServiceConfigurer>{

    public abstract RulesBasedServiceConfigurer getObject() throws Exception;
    
    @Override
    public Class<?> getObjectType() {
        return RulesBasedServiceConfigurer.class;
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
    
    protected Module getModuleByName(Collection<Module> modules, String moduleName) {
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("There are no any module to get rules based service confiurer.");
        }
        if (moduleName == null) {
            return modules.iterator().next();
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

    protected CommonVersion getLastVersionForDeployment(RuleServiceLoader loader, String deploymentName) {
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

    protected RulesInstantiationStrategy getRulesInstantiationStrategy(final String moduleName,
            Collection<Module> modulesInSpecifiedProject) {
        Module necessaryModule = getModuleByName(modulesInSpecifiedProject, moduleName);
        if (necessaryModule == null) {
            throw new IllegalArgumentException(
                    "Incorrect source folder for rules based service configurer has been specified.");
        }
        return RulesInstantiationStrategyFactory.getStrategy(necessaryModule);
    }


}
