package org.openl.rules.ruleservice.conf;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
//*TODO
public class DataSourceRulesBasedServiceConfigurerFactory extends AbstractRulesBasedServiceConfigurerFactoryBean {
    private RuleServiceLoader ruleServiceLoader;
    private String deploymentName;
    private String moduleName;
    private String projectName;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        if (projectName == null) {
            throw new IllegalArgumentException("projectName arg can't be null");
        }

        this.projectName = projectName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        if (moduleName == null) {
            throw new IllegalArgumentException("moduleName arg can't be null");
        }

        this.moduleName = moduleName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName arg can't be null");
        }
        this.deploymentName = deploymentName;
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
    }

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        if (ruleServiceLoader == null) {
            throw new IllegalArgumentException("ruleServiceLoader arg can't be null");
        }
        this.ruleServiceLoader = ruleServiceLoader;
    }

    @Override
    public RulesBasedServiceConfigurer getObject() throws Exception {
        RulesBasedServiceConfigurer configurer = new RulesBasedServiceConfigurer() {
            @Override
            protected RulesInstantiationStrategy getRulesSource() {
                CommonVersion lastVersionForDeployment = getLastVersionForDeployment(ruleServiceLoader, deploymentName);
                if (lastVersionForDeployment == null) {
                    throw new IllegalArgumentException(String.format(
                            "Wrong deployment name has been specified: \"%s\"", deploymentName));
                }
                Collection<Module> modulesInSpecifiedProject = ruleServiceLoader.resolveModulesForProject(
                        deploymentName, lastVersionForDeployment, projectName);
                return getRulesInstantiationStrategy(moduleName, modulesInSpecifiedProject);
            }
        };
        return configurer;
    }
}
