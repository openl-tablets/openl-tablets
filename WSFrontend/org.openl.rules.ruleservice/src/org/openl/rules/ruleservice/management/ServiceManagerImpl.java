package org.openl.rules.ruleservice.management;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.ruleservice.conf.ServiceConfigurer;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.DataSourceListener;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles data source modifications and controls all services.
 *
 * @author PUdalau
 */
public class ServiceManagerImpl implements ServiceManager, DataSourceListener {
    private final Logger log = LoggerFactory.getLogger(ServiceManagerImpl.class);
    private RuleService ruleService;
    private ServiceConfigurer serviceConfigurer;
    private RuleServiceLoader ruleServiceLoader;
    private Map<String, ServiceDescription> serviceDescriptions = new HashMap<>();
    private Map<String, ServiceDescription> failedServiceDescriptions = new HashMap<>();

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
                        "Service '{}' is duplicated! Only one service with this the same name can be deployed! Please, check your configuration!",
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
        for (OpenLService runningService : ruleService.getServices()) {
            String runningServiceName = runningService.getName();
            if (!newServices.containsKey(runningServiceName)) {
                try {
                    ServiceDescription serviceDescription = serviceDescriptions.get(runningServiceName);
                    ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
                    ruleService.undeploy(runningServiceName);
                    serviceDescriptions.remove(runningServiceName);
                    failedServiceDescriptions.remove(runningServiceName);
                } catch (RuleServiceUndeployException e) {
                    log.error("Failed to undeploy '{}' service.", runningServiceName, e);
                } finally {
                    ServiceDescriptionHolder.getInstance().remove();
                }
            }
        }
    }

    private boolean isServiceDeployed(String serviceName) {
        return ruleService.getServiceByName(serviceName) != null;
    }

    protected void deployServices(Map<String, ServiceDescription> newServices) {
        for (ServiceDescription serviceDescription : newServices.values()) {
            String serviceName = serviceDescription.getName();
            try {
                ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
                if (!isServiceDeployed(serviceName)) {
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
                log.error("Failed to deploy '{}' service.", serviceName, e);
            } catch (RuleServiceUndeployException e) {
                log.error("Failed to undeploy '{}' service.", serviceName, e);
            } finally {
                ServiceDescriptionHolder.getInstance().remove();
            }
        }
    }
}
