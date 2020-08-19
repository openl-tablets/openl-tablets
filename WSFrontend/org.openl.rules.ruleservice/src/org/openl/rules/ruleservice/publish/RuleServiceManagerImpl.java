package org.openl.rules.ruleservice.publish;

import java.util.*;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.CompiledOpenClass;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class RuleServiceManagerImpl implements RuleServiceManager, InitializingBean {

    private final Logger log = LoggerFactory.getLogger(RuleServiceManagerImpl.class);

    private Map<String, RuleServicePublisher> supportedPublishers;

    private Collection<String> defaultRuleServicePublishers = Collections.emptyList();

    private final Map<String, OpenLService> services = new HashMap<>();

    private Collection<RuleServicePublisherListener> listeners = Collections.emptyList();

    @Autowired(required = false)
    public void setListeners(Collection<RuleServicePublisherListener> listeners) {
        this.listeners = listeners;
    }

    public void setSupportedPublishers(Map<String, RuleServicePublisher> supportedPublishers) {
        this.supportedPublishers = new TreeMap<>(String::compareToIgnoreCase);
        this.supportedPublishers.putAll(supportedPublishers);
    }

    public void setDefaultRuleServicePublishers(Collection<String> defaultRuleServicePublishers) {
        this.defaultRuleServicePublishers = defaultRuleServicePublishers;
    }

    private void setUrls(OpenLService service) {
        HashMap<String, String> result = new HashMap<>();
        CompiledOpenClass compiledOpenClass = service.getCompiledOpenClass();
        if (compiledOpenClass != null && service.getException() == null && !compiledOpenClass.hasErrors()) {
            supportedPublishers.forEach((id, publisher) -> {
                if (publisher.getServiceByName(service.getName()) != null) {
                    String url = publisher.getUrl(service);
                    result.put(id, url);
                }
            });
        }
        service.setUrls(result);
    }

    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service cannot be null");
        final String serviceName = service.getName();
        Collection<String> sp = service.getPublishers();
        if (CollectionUtils.isEmpty(sp)) {
            sp = defaultRuleServicePublishers;
        }
        Collection<RuleServicePublisher> publishers = new ArrayList<>();
        if (supportedPublishers.size() > 1) {
            for (String p : sp) {
                RuleServicePublisher ruleServicePublisher = supportedPublishers.get(p);
                if (ruleServicePublisher != null) {
                    publishers.add(ruleServicePublisher);
                } else {
                    log.warn("Publisher for '{}' is registered. Please, validate your configuration for service '{}'.",
                        p,
                        service.getName());
                }
            }
            if (publishers.isEmpty()) {
                publishers.addAll(supportedPublishers.values());
            }
        } else {
            publishers.addAll(supportedPublishers.values());
        }
        RuleServiceDeployException e1 = null;
        List<RuleServicePublisher> deployedPublishers = new ArrayList<>();
        for (RuleServicePublisher publisher : publishers) {
            try {
                publisher.deploy(service);
                deployedPublishers.add(publisher);
            } catch (RuleServiceDeployException e) {
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                service.setException(rootCause);
                e1 = e;
                break;
            }
        }
        services.put(serviceName, service);
        if (e1 != null) {
            for (RuleServicePublisher publisher : deployedPublishers) {
                try {
                    publisher.undeploy(service);
                } catch (RuleServiceUndeployException e) {
                    log.error("Failed to undeploy service '{}'.", serviceName, e);
                }
            }
            throw new RuleServiceDeployException("Failed to deploy service.", e1);
        }
        setUrls(service);
        fireDeployListeners(service);
    }

    private void fireDeployListeners(OpenLService service) {
        for (RuleServicePublisherListener listener : listeners) {
            listener.onDeploy(service);
        }
    }

    @Override
    public OpenLService getServiceByName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        return services.get(serviceName);
    }

    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        OpenLService undeployService = services.get(serviceName);
        Objects.requireNonNull(undeployService, "service '" + serviceName + "' has not been found");
        RuleServiceUndeployException e1 = null;
        for (RuleServicePublisher publisher : supportedPublishers.values()) {
            if (publisher.getServiceByName(serviceName) != null) {
                try {
                    publisher.undeploy(undeployService);
                } catch (RuleServiceUndeployException e) {
                    e1 = e;
                }
            }
        }
        if (e1 == null) {
            services.remove(serviceName);
        } else {
            throw new RuleServiceUndeployException("Failed to undeploy a service.", e1);
        }
        fireUndeployListeners(serviceName);
    }

    private void fireUndeployListeners(String serviceName) {
        for (RuleServicePublisherListener listener : listeners) {
            listener.onUndeploy(serviceName);
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (CollectionUtils.isEmpty(supportedPublishers)) {
            throw new BeanInitializationException("You must define at least one supported publisher.");
        }

        for (String defPublisher : defaultRuleServicePublishers) {
            if (!supportedPublishers.containsKey(defPublisher)) {
                throw new BeanInitializationException(
                    String.format("Default publisher with id=%s is not found in the map of supported publishers",
                        defPublisher));
            }
        }
    }
}
