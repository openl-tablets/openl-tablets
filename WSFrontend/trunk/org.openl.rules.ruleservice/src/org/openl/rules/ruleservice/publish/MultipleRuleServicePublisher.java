package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

public class MultipleRuleServicePublisher implements InitializingBean, RuleServicePublisher {

    private Collection<RuleServicePublisher> supportedPublishers = new ArrayList<RuleServicePublisher>();

    private RuleServicePublisher defaultRuleServicePublisher;

    private Map<String, OpenLService> services = new HashMap<String, OpenLService>();

    public Collection<RuleServicePublisher> getSupportedPublishers() {
        return supportedPublishers;
    }

    public void setSupportedPublishers(Collection<RuleServicePublisher> supportedPublishers) {
        this.supportedPublishers = supportedPublishers;
    }

    protected Collection<RuleServicePublisher> dispatch(OpenLService service) {
        Collection<RuleServicePublisher> publishers = new ArrayList<RuleServicePublisher>();
        if (service.getPublishers() == null || service.getPublishers().isEmpty()) {
            publishers.add(getDefaultRuleServicePublisher());
            return publishers;
        }
        for (Class<RuleServicePublisher> clazz : service.getPublishers()) {
            if (getDefaultRuleServicePublisher().getClass().equals(clazz)) {
                publishers.add(getDefaultRuleServicePublisher());
            }
        }
        if (getSupportedPublishers() != null) {
            for (RuleServicePublisher supportedPublisher : getSupportedPublishers()) {
                for (Class<RuleServicePublisher> clazz : service.getPublishers()) {
                    if (supportedPublisher.getClass().equals(clazz)) {
                        publishers.add(supportedPublisher);
                    }
                }
            }
        }
        return publishers;
    }

    public RuleServicePublisher getDefaultRuleServicePublisher() {
        return defaultRuleServicePublisher;
    }

    public void setDefaultRuleServicePublisher(RuleServicePublisher defaultRuleServicePublisher) {
        this.defaultRuleServicePublisher = defaultRuleServicePublisher;
    }

    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Collection<RuleServicePublisher> publishers = dispatch(service);
        for (RuleServicePublisher publisher : publishers) {
            publisher.deploy(service);
        }
        services.put(service.getName(), service);
    }

    @Override
    public OpenLService getServiceByName(String serviceName) {
        return services.get(serviceName);
    }

    @Override
    public Collection<OpenLService> getServices() {
        return services.values();
    }

    @Override
    public void redeploy(OpenLService service) throws RuleServiceRedeployException {
        try {
            undeploy(service.getName());
        } catch (RuleServiceUndeployException e) {
            throw new RuleServiceRedeployException("Service redeploy was failed");
        }
        try {
            deploy(service);
        } catch (RuleServiceDeployException e) {
            throw new RuleServiceRedeployException("Service redeploy was failed");
        }
    }

    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        OpenLService service = services.get(serviceName);
        Collection<RuleServicePublisher> publishers = dispatch(service);
        for (RuleServicePublisher publisher : publishers) {
            publisher.undeploy(serviceName);
        }
        services.remove(serviceName);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getDefaultRuleServicePublisher() == null) {
            throw new BeanInitializationException("You should define default publisher");
        }

        if (getSupportedPublishers() != null || !getSupportedPublishers().isEmpty()) {
            for (RuleServicePublisher p1 : getSupportedPublishers()) {
                for (RuleServicePublisher p2 : getSupportedPublishers()) {
                    if (p1 != p2 && p1.getClass().equals(p2.getClass())) {
                        throw new BeanInitializationException(
                                "Invalid publishers configuration. Two publishers with the same reailzation is not supported.");
                    }
                }
                if (getDefaultRuleServicePublisher().getClass().equals(p1.getClass())) {
                    throw new BeanInitializationException(
                            "Invalid publishers configuration. Two publishers with the same reailzation is not supported.");
                }
            }
        }
    }

}
