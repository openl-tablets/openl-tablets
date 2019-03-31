package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceImpl;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

public class MultipleRuleServicePublisher extends AbstractRuleServicePublisher implements InitializingBean {

    private final Logger log = LoggerFactory.getLogger(RuleServiceImpl.class);

    private Map<String, RuleServicePublisher> supportedPublishers = new TreeMap<>(
        (o1, o2) -> o1.toUpperCase().compareTo(o2.toUpperCase()));

    private Collection<RuleServicePublisher> defaultRuleServicePublishers;

    private Map<String, OpenLService> services = new HashMap<>();

    public Map<String, RuleServicePublisher> getSupportedPublishers() {
        return supportedPublishers;
    }

    public void setSupportedPublishers(Map<String, RuleServicePublisher> supportedPublishers) {
        this.supportedPublishers = new TreeMap<>((o1, o2) -> o1.toUpperCase().compareTo(o2.toUpperCase()));
        this.supportedPublishers.putAll(supportedPublishers);
    }

    protected Collection<RuleServicePublisher> dispatch(OpenLService service) {
        Collection<RuleServicePublisher> publishers = new ArrayList<>();
        if (service.getPublishers() == null || service.getPublishers().isEmpty()) {
            publishers.addAll(getDefaultRuleServicePublishers());
            return publishers;
        }
        if (getSupportedPublishers() != null) {
            for (String key : service.getPublishers()) {
                RuleServicePublisher publisher = supportedPublishers.get(key);
                if (supportedPublishers.get(key) != null) {
                    publishers.add(publisher);
                }
            }
            if (publishers.isEmpty()) {
                publishers.addAll(getDefaultRuleServicePublishers());
            }
        }
        return publishers;
    }

    public Collection<RuleServicePublisher> getDefaultRuleServicePublishers() {
        return defaultRuleServicePublishers;
    }

    public void setDefaultRuleServicePublishers(Collection<RuleServicePublisher> defaultRuleServicePublishers) {
        this.defaultRuleServicePublishers = defaultRuleServicePublishers;
    }

    @Override
    protected void deployService(OpenLService service) throws RuleServiceDeployException {
        Collection<RuleServicePublisher> publishers = dispatch(service);
        RuleServiceDeployException e1 = null;
        for (RuleServicePublisher publisher : publishers) {
            if (!publisher.isServiceDeployed(service.getName())) {
                try {
                    publisher.deploy(service);
                } catch (RuleServiceDeployException e) {
                    e1 = e;
                }
            }
        }
        if (e1 == null) {
            services.put(service.getName(), service);
        } else {
            for (RuleServicePublisher publisher : publishers) {
                if (publisher.isServiceDeployed(service.getName())) {
                    try {
                        publisher.undeploy(service.getName());
                    } catch (RuleServiceUndeployException e) {
                        if (log.isErrorEnabled()) {
                            log.error("Failed to undeploy service '{}' with URL '{}'!",
                                service.getName(),
                                service.getUrl());
                        }
                    }
                }
            }
            throw new RuleServiceDeployException("Failed to deploy service!", e1);
        }
    }

    @Override
    public OpenLService getServiceByName(String serviceName) {
        return services.get(serviceName);
    }

    @Override
    public Collection<OpenLService> getServices() {
        return new ArrayList<>(services.values());
    }

    @Override
    public void undeployService(String serviceName) throws RuleServiceUndeployException {
        OpenLService service = services.get(serviceName);
        Collection<RuleServicePublisher> publishers = dispatch(service);
        RuleServiceUndeployException e1 = null;
        for (RuleServicePublisher publisher : publishers) {
            if (publisher.isServiceDeployed(serviceName)) {
                try {
                    publisher.undeploy(serviceName);
                } catch (RuleServiceUndeployException e) {
                    e1 = e;
                }
            }
        }
        if (e1 == null) {
            services.remove(serviceName);
        } else {
            throw new RuleServiceUndeployException("Failed to undeploy service!", e1);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getDefaultRuleServicePublishers() == null || getDefaultRuleServicePublishers().isEmpty()) {
            throw new BeanInitializationException("You must define at least one default publisher!");
        }
    }

    @Override
    public boolean isServiceDeployed(String name) {
        return getServiceByName(name) != null;
    }

}
