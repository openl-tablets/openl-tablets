package org.openl.rules.ruleservice.databinding;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceRulesDeployObjectFactoryBean extends AbstractFactoryBean<RulesDeploy> {
    @Override
    public Class<?> getObjectType() {
        return RulesDeploy.class;
    }

    @Override
    protected RulesDeploy createInstance() throws Exception {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().get();
        if (serviceDescription == null) {
            throw new ServiceConfigurationException("Failed to locate a service description.");
        }
        return serviceDescription.getRulesDeploy();
    }
}