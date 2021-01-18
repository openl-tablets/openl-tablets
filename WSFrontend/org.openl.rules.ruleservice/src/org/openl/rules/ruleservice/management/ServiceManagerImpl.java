package org.openl.rules.ruleservice.management;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.conf.ServiceConfigurer;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.OpenLService;
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
import org.openl.util.StringUtils;
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

    private ServiceDescription serviceDescriptionInProcess;
    private OpenLService openLServiceInProcess;

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
        this.supportedPublishers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
//                if (services.containsKey(serviceDescription.getName())) {
//                    log.warn(
//                        "Service '{}' is duplicated! Only one service with this the same name can be deployed! Please, check your configuration.",
//                        serviceDescription.getName());
//                } else {
                    services.put(serviceDescription.getDeployPath(), serviceDescription);
//                }
            }
            return services;
        } catch (Exception e) {
            log.error("Failed to gather services to be deployed.", e);
            return Collections.emptyMap();
        }
    }

    private void undeployUnnecessary(Map<String, ServiceDescription> newServices) {
        for (String deployPath : services.keySet().toArray(StringUtils.EMPTY_STRING_ARRAY)) {
            if (!newServices.containsKey(deployPath)) {
                try {
                    undeploy(services.get(deployPath));
                } catch (RuleServiceUndeployException e) {
                    log.error("Failed to undeploy service '{}'.", deployPath, e);
                }
            }
        }
    }

    private void deployServices(Map<String, ServiceDescription> newServices) {
        final Map<DeploymentDescription, List<ServiceDescription>> groupedServices = newServices.values()
            .stream()
            .collect(Collectors.groupingBy(ServiceDescription::getDeployment));
        Lock lock = RuleServiceRedeployLock.getInstance().getWriteLock();
        try {
            lock.lock();
            for (List<ServiceDescription> serviceDescriptionsForDeployment : groupedServices.values()) {
                if (hasAtLeastOneToDeploy(serviceDescriptionsForDeployment)) {
                    for (ServiceDescription serviceDescription : serviceDescriptionsForDeployment) {
                        ServiceDescription old = services.get(serviceDescription.getDeployPath());
                        if (old != null) {
                            try {
                                undeploy(old);
                            } catch (RuleServiceUndeployException e) {
                                log.error("Failed to undeploy service '{}'.", serviceDescription.getName(), e);
                            }
                        }
                    }
                    for (ServiceDescription serviceDescription : serviceDescriptionsForDeployment) {
                        try {
                            deploy(serviceDescription);
                        } catch (RuleServiceDeployException e) {
                            log.error("Failed to deploy service '{}'.", serviceDescription.getName(), e);
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean hasAtLeastOneToDeploy(List<ServiceDescription> serviceDescriptionsForCurrentDeployment) {
        for (ServiceDescription serviceDescription : serviceDescriptionsForCurrentDeployment) {
            ServiceDescription old = services.get(serviceDescription.getName());
            if (old != null) {
                CommonVersion oldVersion = old.getDeployment().getVersion();
                if (oldVersion.compareTo(serviceDescription.getDeployment().getVersion()) != 0) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private void undeploy(ServiceDescription serviceDescription) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceDescription, "service cannot be null");
        String serviceName = serviceDescription.getDeployPath();
        OpenLService service = getServiceByDeploy(serviceName);
        try {
            this.openLServiceInProcess = service;
            this.serviceDescriptionInProcess = serviceDescription;
            undeploy(serviceName);
            log.info("Service '{}' has been undeployed successfully.", serviceName);
        } finally {
            this.openLServiceInProcess = null;
            this.serviceDescriptionInProcess = null;
            startDates.remove(serviceName);
            services.remove(serviceName);
            try {
                ClassLoader classloader = service.getClassLoader();
                OpenClassUtil.releaseClassLoader(classloader);
            } catch (RuleServiceInstantiationException ignored) {
            }
            cleanDeploymentResources(serviceDescription);
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
        String servicePath = serviceDescription.getDeployPath();
        if (getServiceByDeploy(servicePath) != null) {
            throw new RuleServiceDeployException(
                String.format("The service with path '%s' is already deployed.", servicePath));
        }
        try {
            this.serviceDescriptionInProcess = serviceDescription;
            OpenLService newService = ruleServiceInstantiationFactory.createService(serviceDescription);
            this.openLServiceInProcess = newService;
            this.serviceDescriptionInProcess = serviceDescription;
            deploy(newService);
            log.info("Service '{}' has been deployed successfully.", servicePath);
        } catch (RuleServiceInstantiationException e) {
            throw new RuleServiceDeployException("Failed on deploy a service.", e);
        } finally {
            this.serviceDescriptionInProcess = null;
            this.openLServiceInProcess = null;
            // Register a service even it was deployed unsuccessfully.
            services.put(servicePath, serviceDescription);
            startDates.put(servicePath, new Date());
        }
    }

    public XlsModuleOpenClass getXlsModuleOpenClassInProcess() throws RuleServiceInstantiationException {
        return openLServiceInProcess != null ? (XlsModuleOpenClass) openLServiceInProcess.getOpenClass()
                                             : null;
    }

    public RulesDeploy getRulesDeployInProcess() {
        return serviceDescriptionInProcess != null ? serviceDescriptionInProcess.getRulesDeploy() : null;
    }

    public OpenLService getOpenLServiceInProcess() {
        return this.openLServiceInProcess;
    }

    public ServiceDescription getServiceDescriptionInProcess() {
        return this.serviceDescriptionInProcess;
    }

    @Override
    public Collection<String> getServiceErrors(String deployPath) {
        OpenLService service = getServiceByDeploy(deployPath);
        if (service == null) {
            return null;
        }
        CompiledOpenClass openClass = service.getCompiledOpenClass();
        if (openClass != null) {
            Collection<OpenLMessage> messages = openClass.getMessages();
            Collection<OpenLMessage> openLMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages,
                Severity.ERROR);
            List<String> errors = openLMessages.stream().map(OpenLMessage::getSummary).collect(Collectors.toList());
            if (!errors.isEmpty()) {
                return errors;
            }

        }
        Throwable exception = service.getException();
        return exception != null ? Collections.singleton(exception.toString()) : Collections.emptyList();
    }

    @Override
    public Manifest getManifest(String deployPath) {
        ServiceDescription service = services.get(deployPath);
        if (service == null) {
            return null;
        }
        return service.getManifest();
    }

    @Override
    public Collection<MethodDescriptor> getServiceMethods(String deployPath) {
        OpenLService service = getServiceByDeploy(deployPath);
        if (service != null) {
            try {
                return Arrays.stream(service.getServiceClass().getMethods())
                    .map(this::toDescriptor)
                    .sorted(Comparator.comparing(MethodDescriptor::getName, String.CASE_INSENSITIVE_ORDER))
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
            OpenLService serviceByName = getServiceByDeploy(s.getDeployPath());
            Map<String, String> urls = serviceByName != null ? serviceByName.getUrls() : Collections.emptyMap();
            return new ServiceInfo(startDates
                .get(s.getDeployPath()), s.getName(), urls, s.getDeployPath(), s.getManifest() != null);
        })
            .sorted(Comparator.comparing(ServiceInfo::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    private void setUrls(OpenLService service) {
        HashMap<String, String> result = new HashMap<>();
        CompiledOpenClass compiledOpenClass = service.getCompiledOpenClass();
        if (compiledOpenClass != null && service.getException() == null && !compiledOpenClass.hasErrors()) {
            supportedPublishers.forEach((id, publisher) -> {
                if (publisher.getServiceByDeploy(service.getDeployPath()) != null) {
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
        final String servicePath = service.getDeployPath();
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
                    log.warn("Publisher for '{}' is not registered. Please, check the configuration for service '{}'.",
                        p,
                        service.getDeployPath());
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
        services2.put(servicePath, service);
        if (e1 != null) {
            for (RuleServicePublisher publisher : deployedPublishers) {
                try {
                    publisher.undeploy(service);
                } catch (RuleServiceUndeployException e) {
                    log.error("Failed to undeploy service '{}'.", servicePath, e);
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
    public OpenLService getServiceByDeploy(String deployPath) {
        Objects.requireNonNull(deployPath, "servicePath cannot be null");
        return services2.get(deployPath);
    }

    @Override
    public Collection<OpenLService> getServices() {
        return Collections.unmodifiableCollection(services2.values());
    }

    @Override
    public void undeploy(String deployPath) throws RuleServiceUndeployException {
        Objects.requireNonNull(deployPath, "deployPath cannot be null");
        OpenLService undeployService = services2.get(deployPath);
        Objects.requireNonNull(undeployService, String.format("Service '%s' has not been found.", deployPath));
        RuleServiceUndeployException e1 = null;
        for (RuleServicePublisher publisher : supportedPublishers.values()) {
            if (publisher.getServiceByDeploy(deployPath) != null) {
                try {
                    publisher.undeploy(undeployService);
                } catch (RuleServiceUndeployException e) {
                    e1 = e;
                }
            }
        }
        if (e1 == null) {
            services2.remove(deployPath);
        } else {
            throw new RuleServiceUndeployException("Failed to undeploy a service.", e1);
        }
        fireUndeployListeners(deployPath);
    }

    private void fireUndeployListeners(String deployPath) {
        for (RuleServicePublisherListener listener : listeners) {
            listener.onUndeploy(deployPath);
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (CollectionUtils.isEmpty(supportedPublishers)) {
            throw new BeanInitializationException("At least one supported publisher must be registered.");
        }

        for (String defPublisher : defaultRuleServicePublishers) {
            if (!supportedPublishers.containsKey(defPublisher)) {
                throw new BeanInitializationException(
                    String.format("Default publisher with id '%s' is not found in the map of supported publishers.",
                        defPublisher));
            }
        }
    }

    @PreDestroy
    public void destroy() throws Exception {
        undeployUnnecessary(Collections.emptyMap());
    }
}
