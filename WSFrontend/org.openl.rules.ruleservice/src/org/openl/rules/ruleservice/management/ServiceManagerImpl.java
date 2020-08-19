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
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.ruleservice.conf.ServiceConfigurer;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.DataSourceListener;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
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
    private RuleService ruleService;
    private ServiceConfigurer serviceConfigurer;
    private RuleServiceLoader ruleServiceLoader;
    private final Map<String, ServiceDescription> services = new HashMap<>();
    private final Map<String, Date> startDates = new HashMap<>();
    private final Map<String, ServiceDescription> serviceDescriptions = new HashMap<>();
    private final Map<String, ServiceDescription> failedServiceDescriptions = new HashMap<>();

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        if (this.ruleServiceLoader != null) {
            this.ruleServiceLoader.setListener(null);
        }
        this.ruleServiceLoader = ruleServiceLoader;
        if (this.ruleServiceLoader != null) {
            this.ruleServiceLoader.setListener(this);
        }
    }

    public RuleService getRuleService() {
        return ruleService;
    }

    public void setRuleService(RuleService ruleService) {
        this.ruleService = Objects.requireNonNull(ruleService, "ruleService cannot be null");
    }

    public ServiceConfigurer getServiceConfigurer() {
        return serviceConfigurer;
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

    protected Map<String, ServiceDescription> gatherServicesToBeDeployed() {
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

    protected void undeployUnnecessary(Map<String, ServiceDescription> newServices) {
        for (String runningServiceName : services.keySet()) {
            if (!newServices.containsKey(runningServiceName)) {
                try {
                    undeploy(getServiceByName(runningServiceName));
                } catch (RuleServiceUndeployException e) {
                    log.error("Failed to undeploy service '{}'.", runningServiceName, e);
                } finally {
                    ServiceDescriptionHolder.getInstance().remove();
                }
            }
        }
    }

    private void undeploy(OpenLService runningService) throws RuleServiceUndeployException {
        String runningServiceName = runningService.getName();
        ServiceDescription serviceDescription = serviceDescriptions.get(runningServiceName);
        ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
        services.remove(runningServiceName);
        startDates.remove(runningServiceName);
        ruleService.undeploy(runningService);
        serviceDescriptions.remove(runningServiceName);
        failedServiceDescriptions.remove(runningServiceName);
    }

    protected void deployServices(Map<String, ServiceDescription> newServices) {
        for (ServiceDescription serviceDescription : newServices.values()) {
            String serviceName = serviceDescription.getName();
            try {
                ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
                if (!services.containsKey(serviceName)) {
                    ServiceDescription failedServiceDescription = failedServiceDescriptions.get(serviceName);
                    if (failedServiceDescription != null) {
                        if (!serviceName.equals(failedServiceDescription.getName())) {
                            throw new IllegalStateException();
                        }
                        if (failedServiceDescription.getDeployment()
                            .getVersion()
                            .compareTo(serviceDescription.getDeployment().getVersion()) != 0) {
                            failedServiceDescriptions.remove(serviceName);
                            ruleService.deploy(serviceDescription);
                            serviceDescriptions.put(serviceName, serviceDescription);
                        }
                    } else {
                        ruleService.deploy(serviceDescription);
                        serviceDescriptions.put(serviceName, serviceDescription);
                    }
                } else {
                    ruleService.redeploy(serviceDescription);
                    serviceDescriptions.put(serviceName, serviceDescription);
                }
            } catch (RuleServiceDeployException e) {
                failedServiceDescriptions.put(serviceName, serviceDescription);
                log.error("Failed to deploy service '{}'.", serviceName, e);
            } catch (RuleServiceUndeployException e) {
                log.error("Failed to undeploy service '{}'.", serviceName, e);
            } finally {
                ServiceDescriptionHolder.getInstance().remove();
                services.put(serviceName, serviceDescription);
                startDates.put(serviceName, new Date());
            }
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
        return ruleService.getServiceByName(serviceName);
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
        return services.values()
                .stream()
                .map(s -> {
                    OpenLService serviceByName = getServiceByName(s.getName());
                    Map<String, String> urls = serviceByName != null ? serviceByName.getUrls() : Collections.emptyMap();
                    return new ServiceInfo(startDates.get(s.getName()), s.getName(), urls, s.getServicePath(), s.getManifest() != null);
                })
                .sorted(Comparator.comparing(ServiceInfo::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
}
