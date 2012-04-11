package org.openl.rules.ruleservice.management;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.core.IRuleService;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.ServiceDeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.IDataSourceListener;
import org.openl.rules.ruleservice.loader.IRulesLoader;

/**
 * Handles data source modifications and controls all services
 * 
 * @author PUdalau
 * 
 */
public class ServiceManager implements IServiceManager, IDataSourceListener {
    private Log log = LogFactory.getLog(ServiceManager.class);
    private IRuleService ruleService;
    private IServiceConfigurer serviceConfigurer;
    private IRulesLoader rulesLoader;

    public void setRulesLoader(IRulesLoader rulesLoader) {
        this.rulesLoader = rulesLoader;
        if (this.rulesLoader != null) {
            try {
                rulesLoader.getDataSource().removeListener(this);
            } catch (UnsupportedOperationException e) {
            }
        }
        if (this.rulesLoader.getDataSource() != null) {
            try {
                this.rulesLoader.getDataSource().addListener(this);
            } catch (UnsupportedOperationException e) {
            }
        } else {
            throw new IllegalArgumentException("The should be defined the data source in rules loader");
        }
    }

    public IRulesLoader getRulesLoader() {
        return rulesLoader;
    }

    public IRuleService getRuleService() {
        return ruleService;
    }

    public void setRuleService(IRuleService ruleService) {
        if (ruleService == null) {
            throw new IllegalArgumentException("ruleService can't be null");
        }
        this.ruleService = ruleService;
    }

    public IServiceConfigurer getServiceConfigurer() {
        return serviceConfigurer;
    }

    public void setServiceConfigurer(IServiceConfigurer serviceConfigurer) {
        if (serviceConfigurer == null) {
            throw new IllegalArgumentException("serviceConfigurer can't be null");
        }

        this.serviceConfigurer = serviceConfigurer;
    }

    /**
     * Determine services to be deployed on start.
     */
    public void start() {
        log.info("Assembling services after service manager start");
        processServices();
    }

    public void onDeploymentAdded() {
        log.info("Assembling services after data source modification");
        synchronized (this) {
            processServices();
        }
    }

    private void processServices() {
        List<ServiceDescription> servicesToBeDeployed = serviceConfigurer.getServicesToBeDeployed(getRulesLoader());
        Map<String, ServiceDescription> newServices = new HashMap<String, ServiceDescription>();
        for (ServiceDescription serviceDescription : servicesToBeDeployed) {
            newServices.put(serviceDescription.getName(), serviceDescription);
        }

        undeployUnnecessary(newServices);
        redeployExisitng(newServices);
        deployNewServices(newServices);
    }

    private void undeployUnnecessary(Map<String, ServiceDescription> newServices) {
        for (OpenLService runningService : ruleService.getRunningServices()) {
            String serviceName = runningService.getName();
            if (!newServices.containsKey(serviceName)) {
                try {
                    ruleService.undeploy(serviceName);
                } catch (ServiceDeployException e) {
                    log.error(String.format("Failed to undeploy \"%s\" service", serviceName), e);
                }
            }
        }
    }

    private boolean isServiceExists(String serviceName) {
        return ruleService.findServiceByName(serviceName) != null;
    }

    private void redeployExisitng(Map<String, ServiceDescription> newServices) {
        for (ServiceDescription serviceDescription : newServices.values()) {
            if (isServiceExists(serviceDescription.getName())) {
                try {
                    ruleService.redeploy(serviceDescription);
                } catch (ServiceDeployException e) {
                    log.error(String.format("Failed to redeploy \"%s\" service", serviceDescription.getName()), e);
                }
            }
        }
    }

    private void deployNewServices(Map<String, ServiceDescription> newServices) {
        for (ServiceDescription serviceDescription : newServices.values()) {
            if (!isServiceExists(serviceDescription.getName())) {
                try {
                    ruleService.deploy(serviceDescription);
                } catch (ServiceDeployException e) {
                    log.error(String.format("Failed to deploy \"%s\" service", serviceDescription.getName()), e);
                }
            }
        }
    }

}
