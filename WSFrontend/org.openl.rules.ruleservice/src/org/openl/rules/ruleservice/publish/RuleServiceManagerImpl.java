package org.openl.rules.ruleservice.publish;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.servlet.AvailableServicesPresenter;
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
        Map<String, ServiceInfo> serviceInfos = new TreeMap<>();
        // Wrapped into collection of publishers
        for (RuleServicePublisher p : supportedPublishers.values()) {
            collectServicesInfo(serviceInfos, p);
        }
        return serviceInfos.values();
    }

    private void collectServicesInfo(Map<String, ServiceInfo> servicesInfo, RuleServicePublisher publisher) {
        if (publisher instanceof AvailableServicesPresenter) {
            List<ServiceInfo> services = ((AvailableServicesPresenter) publisher).getAvailableServices();
            for (ServiceInfo serviceInfo : services) {
                String serviceName = serviceInfo.getName();
                ServiceInfo current = servicesInfo.get(serviceName);
                if (current == null) {
                    servicesInfo.put(serviceName, serviceInfo);
                } else {
                    // Join urls
                    Map<String, String> urls = new TreeMap<>(current.getUrls());
                    urls.putAll(serviceInfo.getUrls());

                    // Select the latest time
                    Date startedTime = current.getStartedTime();
                    Date newStartedTime = serviceInfo.getStartedTime();
                    if (startedTime.before(newStartedTime)) {
                        startedTime = newStartedTime;
                    }

                    ServiceInfo newServiceInfo = new ServiceInfo(startedTime, serviceName, urls, serviceInfo.getServicePath());
                    servicesInfo.put(serviceName, newServiceInfo);
                }
            }
        }
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
        Objects.requireNonNull(service, "service argument must not be null!");
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
            if (publisher.getServiceByName(serviceName) == null) {
                try {
                    publisher.deploy(service);
                    deployedPublishers.add(publisher);
                } catch (RuleServiceDeployException e) {
                    e1 = e;
                    break;
                }
            }
        }
        if (e1 == null) {
            services.put(serviceName, service);
        } else {
            for (RuleServicePublisher publisher : deployedPublishers) {
                if (publisher.getServiceByName(serviceName) != null) {
                    try {
                        publisher.undeploy(serviceName);
                    } catch (RuleServiceUndeployException e) {
                        log.error("Failed to undeploy service '{}'!", serviceName, e);
                    }
                }
            }
            throw new RuleServiceDeployException("Failed to deploy service!", e1);
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
        Objects.requireNonNull(serviceName, "serviceName argument must not be null!");
        return services.get(serviceName);
    }

    @Override
    public Collection<OpenLService> getServices() {
        return new ArrayList<>(services.values());
    }

    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceName, "serviceName argument must not be null!");
        RuleServiceUndeployException e1 = null;
        for (RuleServicePublisher publisher : supportedPublishers.values()) {
            if (publisher.getServiceByName(serviceName) != null) {
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
            throw new BeanInitializationException("You must define at least one supported publisher!");
        }

        for (String defPublisher : defaultRuleServicePublishers) {
            if (!supportedPublishers.containsKey(defPublisher)) {
                throw new BeanInitializationException(
                    "Default publisher with id=" + defPublisher + " has not been found in the map of supported publishers");
            }
        }
    }
}
