package org.openl.rules.ruleservice.conf;

import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.springframework.beans.factory.FactoryBean;

import java.util.Collection;

public abstract class AbstractRulesBasedServiceConfigurerFactoryBean
        implements FactoryBean<RulesBasedServiceConfigurer> {

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
