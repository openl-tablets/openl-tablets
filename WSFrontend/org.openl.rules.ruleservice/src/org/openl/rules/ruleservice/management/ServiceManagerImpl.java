package org.openl.rules.ruleservice.management;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

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
import org.openl.rules.ruleservice.publish.RuleServiceManager;
import org.openl.rules.ruleservice.servlet.MethodDescriptor;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.openl.rules.ruleservice.servlet.ServiceInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles data source modifications and controls all services.
 *
 * @author PUdalau
 */
public class ServiceManagerImpl implements ServiceManager, DataSourceListener, ServiceInfoProvider {
    private final Logger log = LoggerFactory.getLogger(ServiceManagerImpl.class);
    private RuleServiceManager ruleServiceManager;
    private RuleServiceInstantiationFactory ruleServiceInstantiationFactory;
    private ServiceConfigurer serviceConfigurer;
    private RuleServiceLoader ruleServiceLoader;
    private final Map<String, ServiceDescription> services = new HashMap<>();
    private final Map<String, Date> startDates = new HashMap<>();

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        if (this.ruleServiceLoader != null) {
            this.ruleServiceLoader.setListener(null);
        }
        this.ruleServiceLoader = ruleServiceLoader;
        if (this.ruleServiceLoader != null) {
            this.ruleServiceLoader.setListener(this);
        }
    }

    public void setRuleServiceManager(RuleServiceManager ruleServiceManager) {
        this.ruleServiceManager = Objects.requireNonNull(ruleServiceManager, "ruleServiceManager cannot be null");
    }

    public void setRuleServiceInstantiationFactory(RuleServiceInstantiationFactory ruleServiceInstantiationFactory) {
        this.ruleServiceInstantiationFactory = Objects.requireNonNull(ruleServiceInstantiationFactory,
            "ruleServiceInstantiationFactory cannot be null");
    }

    public void setServiceConfigurer(ServiceConfigurer serviceConfigurer) {
        this.serviceConfigurer = Objects.requireNonNull(serviceConfigurer, "serviceConfigurer cannot be null");
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
                ruleServiceManager.undeploy(serviceName);
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
        OpenLService service = ruleServiceManager.getServiceByName(serviceName);
        if (service != null) {
            throw new RuleServiceDeployException(
                String.format("The service with name '%s' has already been deployed.", serviceName));
        }
        // Some singleton caches may not be cleaned by calling undeploy method
        cleanDeploymentResources(serviceDescription);
        try {
            ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
            OpenLService newService = ruleServiceInstantiationFactory.createService(serviceDescription);
            OpenLServiceHolder.getInstance().setOpenLService(newService);
            ruleServiceManager.deploy(newService);
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

    private OpenLService getServiceByName(String serviceName) {
        return ruleServiceManager.getServiceByName(serviceName);
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
}
