package org.openl.rules.ruleservice.management;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.ruleservice.conf.ServiceConfigurer;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationFactory;
import org.openl.rules.ruleservice.core.RuleServiceRedeployLock;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.DataSourceListener;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.publish.RuleServicePublisherListener;
import org.openl.rules.ruleservice.servlet.MethodDescriptor;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.openl.rules.ruleservice.servlet.ServiceInfoProvider;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles data source modifications and controls all services.
 *
 * @author PUdalau
 */
public class ServiceManagerImpl implements ServiceManager, DataSourceListener, ServiceInfoProvider, InitializingBean {
    private final Logger log = LoggerFactory.getLogger(ServiceManagerImpl.class);
    private RuleServiceInstantiationFactory ruleServiceInstantiationFactory;
    private ServiceConfigurer serviceConfigurer;
    private RuleServiceLoader ruleServiceLoader;
    private final Map<String, ServiceDescription> services = new HashMap<>();
    private final Map<String, OpenLService> services2 = new HashMap<>();
    private final Map<String, Date> startDates = new HashMap<>();

    private Map<String, RuleServicePublisher> supportedPublishers;
    private Collection<String> defaultRuleServicePublishers = Collections.emptyList();
    private Collection<RuleServicePublisherListener> listeners = Collections.emptyList();

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        if (this.ruleServiceLoader != null) {
            this.ruleServiceLoader.setListener(null);
        }
        this.ruleServiceLoader = ruleServiceLoader;
        if (this.ruleServiceLoader != null) {
            this.ruleServiceLoader.setListener(this);
        }
    }

    public void setRuleServiceInstantiationFactory(RuleServiceInstantiationFactory ruleServiceInstantiationFactory) {
        this.ruleServiceInstantiationFactory = Objects.requireNonNull(ruleServiceInstantiationFactory,
            "ruleServiceInstantiationFactory cannot be null");
    }

    public void setServiceConfigurer(ServiceConfigurer serviceConfigurer) {
        this.serviceConfigurer = Objects.requireNonNull(serviceConfigurer, "serviceConfigurer cannot be null");
    }


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

    /**
     * Determine services to be deployed on start.
     */
    @Override
    public void start() {
        log.info("Assembling services after service manager start.");
        processServices();
    }

    @Override
    public void onDeploymentAdded() {
        log.info("Assembling services after data source modification.");
        processServices();
    }

    private synchronized void processServices() {
        Map<String, ServiceDescription> newServices = gatherServicesToBeDeployed();
        undeployUnnecessary(newServices);
        deployServices(newServices);
    }

    private Map<String, ServiceDescription> gatherServicesToBeDeployed() {
        try {
            Collection<ServiceDescription> servicesToBeDeployed = serviceConfigurer
                .getServicesToBeDeployed(ruleServiceLoader);
            Map<String, ServiceDescription> services = new HashMap<>();
            for (ServiceDescription serviceDescription : servicesToBeDeployed) {
                if (services.containsKey(serviceDescription.getName())) {
                    log.warn(
                        "Service '{}' is duplicated! Only one service with this the same name can be deployed! Please, check your configuration.",
                        serviceDescription.getName());
                } else {
                    services.put(serviceDescription.getName(), serviceDescription);
                }
            }
            return services;
        } catch (Exception e) {
            log.error("Failed to gather services to be deployed.", e);
            return Collections.emptyMap();
        }
    }

    private void undeployUnnecessary(Map<String, ServiceDescription> newServices) {
        for (Map.Entry<String, ServiceDescription> svc : services.entrySet()) {
            String serviceName = svc.getKey();
            ServiceDescription service = svc.getValue();
            if (!newServices.containsKey(serviceName)) {
                try {
                    undeploy(service);
                } catch (RuleServiceUndeployException e) {
                    log.error("Failed to undeploy service '{}'.", serviceName, e);
                }
            }
        }
    }

    private void deployServices(Map<String, ServiceDescription> newServices) {
        for (ServiceDescription serviceDescription : newServices.values()) {
            String serviceName = serviceDescription.getName();
            ServiceDescription old = services.get(serviceName);
            try {

                if (old != null) {
                    CommonVersion oldVersion = old.getDeployment().getVersion();
                    if (oldVersion.compareTo(serviceDescription.getDeployment().getVersion()) != 0) {
                        Lock lock = RuleServiceRedeployLock.getInstance().getWriteLock();
                        try {
                            lock.lock();
                            // Do redeploy
                            undeploy(old);
                            deploy(serviceDescription);
                        } finally {
                            lock.unlock();
                        }
                    }
                } else {
                    deploy(serviceDescription);
                }
            } catch (RuleServiceDeployException e) {
                log.error("Failed to deploy service '{}'.", serviceName, e);
            } catch (RuleServiceUndeployException e) {
                log.error("Failed to undeploy service '{}'.", serviceName, e);
            }
        }
    }

    private void undeploy(ServiceDescription serviceDescription) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceDescription, "service cannot be null");
        String serviceName = serviceDescription.getName();
        OpenLService service = getServiceByName(serviceName);
        try {
            OpenLServiceHolder.getInstance().setOpenLService(service);
            ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
            try {
                undeploy(serviceName);
            } finally {
                cleanDeploymentResources(serviceDescription);
                ClassLoader classloader = null;
                try {
                    classloader = service.getClassLoader();
                } catch (RuleServiceInstantiationException ignored) {
                }
                OpenClassUtil.releaseClassLoader(classloader);
            }
            log.info("Service '{}' was undeployed successfully.", serviceName);
            startDates.remove(serviceName);
            services.remove(serviceName);
        } finally {
            ServiceDescriptionHolder.getInstance().remove();
            OpenLServiceHolder.getInstance().remove();
        }
    }

    private void cleanDeploymentResources(ServiceDescription serviceDescription) {
        boolean foundServiceWithThisDeployment = false;
        for (ServiceDescription sd : services.values()) {
            if (sd.getDeployment().equals(serviceDescription.getDeployment())) {
                foundServiceWithThisDeployment = true;
                break;
            }
        }
        if (!foundServiceWithThisDeployment) {
            ruleServiceInstantiationFactory.clean(serviceDescription);
        }
    }

    private void deploy(ServiceDescription serviceDescription) throws RuleServiceDeployException {
        String serviceName = serviceDescription.getName();
        OpenLService service = getServiceByName(serviceName);
        if (service != null) {
            throw new RuleServiceDeployException(
                String.format("The service with name '%s' has already been deployed.", serviceName));
        }
        try {
            ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
            OpenLService newService = ruleServiceInstantiationFactory.createService(serviceDescription);
            OpenLServiceHolder.getInstance().setOpenLService(newService);
            deploy(newService);
            log.info("Service '{}' has been deployed successfully.", serviceName);
        } catch (RuleServiceInstantiationException e) {
            throw new RuleServiceDeployException("Failed on deploy a service.", e);
        } finally {
            cleanDeploymentResources(serviceDescription);
            ServiceDescriptionHolder.getInstance().remove();
            OpenLServiceHolder.getInstance().remove();

            // Register a service even it was deployed unsuccessfully.
            services.put(serviceName, serviceDescription);
            startDates.put(serviceName, new Date());
        }
    }

    @Override
    public Collection<String> getServiceErrors(String serviceName) {
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            return null;
        }
        Collection<OpenLMessage> messages = service.getCompiledOpenClass().getMessages();
        Collection<OpenLMessage> openLMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
        List<String> errors = openLMessages.stream().map(OpenLMessage::getSummary).collect(Collectors.toList());
        if (errors.isEmpty()) {
            Throwable exception = service.getException();
            errors.add(exception.toString());
        }
        return errors;
    }

    @Override
    public Manifest getManifest(String serviceName) {
        ServiceDescription service = services.get(serviceName);
        if (service == null) {
            return null;
        }
        return service.getManifest();
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
    public Collection<ServiceInfo> getServicesInfo() {
        return services.values().stream().map(s -> {
            OpenLService serviceByName = getServiceByName(s.getName());
            Map<String, String> urls = serviceByName != null ? serviceByName.getUrls() : Collections.emptyMap();
            return new ServiceInfo(startDates
                .get(s.getName()), s.getName(), urls, s.getServicePath(), s.getManifest() != null);
        })
            .sorted(Comparator.comparing(ServiceInfo::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
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
        services2.put(serviceName, service);
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
        return services2.get(serviceName);
    }

    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        OpenLService undeployService = services2.get(serviceName);
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
            services2.remove(serviceName);
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
