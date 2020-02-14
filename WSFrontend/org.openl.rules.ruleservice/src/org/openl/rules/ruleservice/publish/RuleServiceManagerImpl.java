package org.openl.rules.ruleservice.publish;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.servlet.MethodDescriptor;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
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

    private Map<String, OpenLService> services = new HashMap<>();
    private Map<String, Date> startDates = new HashMap<>();

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

    @Override
    public Collection<ServiceInfo> getServicesInfo() {
        return services.values()
            .stream()
            .map(s -> new ServiceInfo(startDates.get(s.getName()), s.getName(), getUrls(s), s.getServicePath()))
            .sorted(Comparator.comparing(ServiceInfo::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    private Map<String, String> getUrls(OpenLService service) {
        HashMap<String, String> result = new HashMap<>();
        if (service.getException() == null && !service.getCompiledOpenClass().hasErrors()) {
            supportedPublishers.forEach((id, publisher) -> {
                if (publisher.getServiceByName(service.getName()) != null) {
                    String url = publisher.getUrl(service);
                    result.put(id, url);
                }
            });
        }
        return result;
    }

    @Override
    public Collection<MethodDescriptor> getServiceMethods(String serviceName) {
        OpenLService service = getServiceByName(serviceName);
        if (service != null) {
            try {
                return Arrays.stream(service.getServiceClass().getMethods())
                    .map(this::toDescriptor)
                    .sorted(Comparator.comparing(MethodDescriptor::getName, String::compareToIgnoreCase))
                    .collect(Collectors.toList());
            } catch (RuleServiceInstantiationException ignore) {
                // Ignore
            }
        }
        return null;
    }

    @Override
    public List<String> getServiceErrors(String serviceName) {
        OpenLService service = services.get(serviceName);
        Collection<OpenLMessage> messages = service.getCompiledOpenClass().getMessages();
        Collection<OpenLMessage> openLMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
        List<String> errors = openLMessages.stream().map(OpenLMessage::getSummary).collect(Collectors.toList());
        if (errors.isEmpty()) {
            Throwable exception = service.getException();
            errors.add(exception.toString());
        }
        return errors;
    }

    private MethodDescriptor toDescriptor(Method method) {
        String name = method.getName();
        String returnType = method.getReturnType().getSimpleName();
        List<String> paramTypes = Arrays.stream(method.getParameterTypes())
            .map(Class::getSimpleName)
            .collect(Collectors.toList());
        return new MethodDescriptor(name, returnType, paramTypes);
    }

    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service cannot be null");
        final String serviceName = service.getName();
        Collection<String> sp = service.getPublishers();
        if (CollectionUtils.isEmpty(sp)) {
            sp = defaultRuleServicePublishers;
        }
        Collection<RuleServicePublisher> publishers = sp.stream()
            .map(supportedPublishers::get)
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(publishers)) {
            publishers = supportedPublishers.values();
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
        startDates.put(serviceName, new Date());
        services.put(serviceName, service);
        if (e1 != null) {
            for (RuleServicePublisher publisher : deployedPublishers) {
                if (publisher.getServiceByName(serviceName) != null) {
                    try {
                        publisher.undeploy(service);
                    } catch (RuleServiceUndeployException e) {
                        log.error("Failed to undeploy service '{}'.", serviceName, e);
                    }
                }
            }
            throw new RuleServiceDeployException("Failed to deploy service.", e1);
        }
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
    public Collection<OpenLService> getServices() {
        return new ArrayList<>(services.values());
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
            startDates.remove(serviceName);
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
