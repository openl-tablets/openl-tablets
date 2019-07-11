package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Collection;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;

public abstract class AbstractRuleServicePublisher implements RuleServicePublisher {
    protected Collection<RuleServicePublisherListener> listeners = new ArrayList<>();

    
    private PublisherType publisherType;

    public void setPublisherType(PublisherType publisherType) {
        this.publisherType = publisherType;
    }

    public PublisherType getPublisherType() {
        return publisherType;
    }

    protected void fireDeployListeners(OpenLService service) {
        for (RuleServicePublisherListener listener : listeners) {
            listener.onDeploy(service);
        }
    }

    protected void fireUndeployListeners(String serviceName) {
        for (RuleServicePublisherListener listener : listeners) {
            listener.onUndeploy(serviceName);
        }
    }

    protected String processURL(String url) {
        String ret = url;
        while (ret.charAt(0) == '/') {
            ret = ret.substring(1);
        }
        return URLHelper.processURL(ret);
    }

    public void setListeners(Collection<RuleServicePublisherListener> listeners) {
        this.listeners = listeners;
    }

    public Collection<RuleServicePublisherListener> getListeners() {
        return listeners;
    }
    
    @Override
    public boolean isServiceDeployed(String name) {
        return getServiceByName(name) != null;
    }
    
    @Override
    public void addListener(RuleServicePublisherListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void removeListener(RuleServicePublisherListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    protected abstract void deployService(OpenLService service) throws RuleServiceDeployException;

    @Override
    public final void deploy(OpenLService service) throws RuleServiceDeployException {
        deployService(service);
        fireDeployListeners(service);
    }

    protected abstract void undeployService(String serviceName) throws RuleServiceUndeployException;

    @Override
    public final void undeploy(String serviceName) throws RuleServiceUndeployException {
        undeployService(serviceName);
        fireUndeployListeners(serviceName);
    }
}
