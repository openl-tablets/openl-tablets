package org.openl.rules.ruleservice.management;

import org.openl.OpenL;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.rules.ruleservice.conf.ServiceConfigurer;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.DataSourceListener;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private Map<String, ServiceDescription> serviceDescriptions = new HashMap<String, ServiceDescription>();

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        if (this.ruleServiceLoader != null) {
            this.ruleServiceLoader.removeListener(this);
        }
        this.ruleServiceLoader = ruleServiceLoader;
        if (this.ruleServiceLoader != null) {
            this.ruleServiceLoader.addListener(this);
        }
    }

    public RuleService getRuleService() {
        return ruleService;
    }

    public void setRuleService(RuleService ruleService) {
        if (ruleService == null) {
            throw new IllegalArgumentException("ruleService can't be null");
        }
        this.ruleService = ruleService;
    }

    public ServiceConfigurer getServiceConfigurer() {
        return serviceConfigurer;
    }

    public void setServiceConfigurer(ServiceConfigurer serviceConfigurer) {
        if (serviceConfigurer == null) {
            throw new IllegalArgumentException("serviceConfigurer can't be null");
        }

        this.serviceConfigurer = serviceConfigurer;
    }

    /**
     * Determine services to be deployed on start.
     */
    @Override
    public void start() {
        log.info("Assembling services after service manager start");
        processServices();
    }

    @Override
    public void onDeploymentAdded() {
        log.info("Assembling services after data source modification");
        processServices();
    }

    private synchronized void processServices() {
        resetOpenL();

        Map<String, ServiceDescription> newServices = gatherServicesToBeDeployed();
        undeployUnnecessary(newServices);
        deployServices(newServices);
    }

    protected Map<String, ServiceDescription> gatherServicesToBeDeployed() {
        try {
            Collection<ServiceDescription> servicesToBeDeployed = serviceConfigurer.getServicesToBeDeployed(
                    ruleServiceLoader);
            Map<String, ServiceDescription> services = new HashMap<String, ServiceDescription>();
            for (ServiceDescription serviceDescription : servicesToBeDeployed) {
                if (services.containsKey(serviceDescription.getName())) {
                    log.warn(
                            "Service with name \"{}\" is duplicated! Only one service with this name will be deployed! Please, check your configuration!",
                            serviceDescription.getName());
                } else {
                    services.put(serviceDescription.getName(), serviceDescription);
                }
            }
            return services;
        } catch (Exception e) {
            log.error("Failed to gather services to be deployed", e);
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
                } catch (RuleServiceUndeployException e) {
                    log.error("Failed to undeploy \"{}\" service", runningServiceName, e);
                } finally {
                    ServiceDescriptionHolder.getInstance().remove();
                }
            }
        }
    }

    private boolean isServiceExists(String serviceName) {
        return ruleService.getServiceByName(serviceName) != null;
    }

    protected void deployServices(Map<String, ServiceDescription> newServices) {
        for (ServiceDescription serviceDescription : newServices.values()) {
            try {
                ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
                if (!isServiceExists(serviceDescription.getName())) {
                    ruleService.deploy(serviceDescription);
                } else {
                    ruleService.redeploy(serviceDescription);
                }
                serviceDescriptions.put(serviceDescription.getName(), serviceDescription);
            } catch (RuleServiceDeployException e) {
                log.error("Failed to deploy \"{}\" service", serviceDescription.getName(), e);
            } catch (RuleServiceRedeployException e) {
                log.error("Failed to redeploy \"{}\" service", serviceDescription.getName(), e);
            } finally {
                ServiceDescriptionHolder.getInstance().remove();
            }

        }
    }

    private void resetOpenL() {
        // TODO Refactor the classes below to not have static HashMap fields: it's bad for multithreading and memory-leak prone.
        OpenL.reset();
        OpenLConfiguration.reset();
        ClassLoaderFactory.reset();
    }

}
