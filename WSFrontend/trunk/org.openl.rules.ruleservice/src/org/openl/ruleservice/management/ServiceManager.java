package org.openl.ruleservice.management;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.ruleservice.OpenLService;
import org.openl.ruleservice.RuleService;
import org.openl.ruleservice.ServiceDeployException;
import org.openl.ruleservice.ServiceDescription;
import org.openl.ruleservice.loader.IDataSourceListener;

/**
 * Handles data source modifications and controls all services
 * 
 * @author PUdalau
 * 
 */
public class ServiceManager implements IDataSourceListener {
    private static final Log LOG = LogFactory.getLog(ServiceManager.class);
    private RuleService ruleService;
    private IServiceConfigurer serviceConfigurer;
    
    public RuleService getRuleService() {
        return ruleService;
    }
    
    public void setRuleService(RuleService ruleService) {
        this.ruleService = ruleService;
    }
    
    public IServiceConfigurer getServiceConfigurer() {
        return serviceConfigurer;
    }
    
    public void setServiceConfigurer(IServiceConfigurer serviceConfigurer) {
        this.serviceConfigurer = serviceConfigurer;
    }

    /**
     * Determine services to be deployed on start.
     */
    public void start() {
        LOG.info("Assembling services after service manager start");
        processServices();
    }

    public void onDeploymentAdded() {
        LOG.info("Assembling services after data source modification");
        processServices();
    }

    private void processServices() {
        List<ServiceDescription> servicesToBeDeployed = serviceConfigurer.getServicesToBeDeployed(ruleService
                .getLoader());
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
                    LOG.error(String.format("Failed to undeploy \"%s\" service", serviceName), e);
                }
            }
        }
    }

    private boolean isServiceExists(String serviceName) {
        return ruleService.findServiceByName(serviceName) != null;
    }

    private void redeployExisitng(Map<String, ServiceDescription> newServices) {
        for (String serviceName : newServices.keySet()) {
            if (isServiceExists(serviceName)) {
                try {
                    ruleService.redeploy(newServices.get(serviceName));
                } catch (ServiceDeployException e) {
                    LOG.error(String.format("Failed to redeploy \"%s\" service", serviceName), e);
                }
            }
        }
    }

    private void deployNewServices(Map<String, ServiceDescription> newServices) {
        for (String serviceName : newServices.keySet()) {
            if (!isServiceExists(serviceName)) {
                try {
                    ruleService.deploy(newServices.get(serviceName));
                } catch (ServiceDeployException e) {
                    LOG.error(String.format("Failed to deploy \"%s\" service", serviceName), e);
                }
            }
        }
    }

}
