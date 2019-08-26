package org.openl.rules.ruleservice.publish;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;

public abstract class AbstractRuleServicePublisher implements RuleServicePublisher {
    private PublisherType publisherType;

    public void setPublisherType(PublisherType publisherType) {
        this.publisherType = publisherType;
    }

    public PublisherType getPublisherType() {
        return publisherType;
    }

    @Override
    public boolean isServiceDeployed(String name) {
        return getServiceByName(name) != null;
    }

    protected abstract void deployService(OpenLService service) throws RuleServiceDeployException;

    @Override
    public final void deploy(OpenLService service) throws RuleServiceDeployException {
        deployService(service);
    }

    protected abstract void undeployService(String serviceName) throws RuleServiceUndeployException;

    @Override
    public final void undeploy(String serviceName) throws RuleServiceUndeployException {
        undeployService(serviceName);
    }
}
