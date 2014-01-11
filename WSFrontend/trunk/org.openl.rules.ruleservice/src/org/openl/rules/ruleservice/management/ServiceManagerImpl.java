package org.openl.rules.ruleservice.management;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.OpenL;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.rules.ruleservice.conf.ServiceConfigurer;
import org.openl.rules.ruleservice.core.DeploymentRelatedInfoCache;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.DataSourceListener;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

/**
 * Handles data source modifications and controls all services.
 * 
 * @author PUdalau
 * 
 */
public class ServiceManagerImpl implements ServiceManager, DataSourceListener {
    private final Log log = LogFactory.getLog(ServiceManagerImpl.class);
    private RuleService ruleService;
    private ServiceConfigurer serviceConfigurer;
    private RuleServiceLoader ruleServiceLoader;

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        this.ruleServiceLoader = ruleServiceLoader;
        if (this.ruleServiceLoader != null) {
            try {
                ruleServiceLoader.getDataSource().removeListener(this);
            } catch (UnsupportedOperationException e) {
            }
        }
        if (this.ruleServiceLoader.getDataSource() != null) {
            try {
                this.ruleServiceLoader.getDataSource().addListener(this);
            } catch (UnsupportedOperationException e) {
            }
        } else {
            throw new IllegalArgumentException("The should be defined the data source in rules loader");
        }
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
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
    public void start() {
        if (log.isInfoEnabled()) {
            log.info("Assembling services after service manager start");
        }
        synchronized (this) {
            processServices();
        }
    }

    public void onDeploymentAdded() {
        if (log.isInfoEnabled()) {
            log.info("Assembling services after data source modification");
        }
        synchronized (this) {
            processServices();
        }
    }

    private void processServices() {
        try {
            resetOpenL();

            DeploymentRelatedInfoCache.setInstance(new DeploymentRelatedInfoCache());
            Map<String, ServiceDescription> newServices = gatherServicesToBeDeployed();
            undeployUnnecessary(newServices);
            redeployExisitng(newServices);
            deployNewServices(newServices);
        } finally {
            DeploymentRelatedInfoCache.removeInstance();
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, ServiceDescription> gatherServicesToBeDeployed() {
        try {
            Collection<ServiceDescription> servicesToBeDeployed = serviceConfigurer
                    .getServicesToBeDeployed(getRuleServiceLoader());
            Map<String, ServiceDescription> newServices = new HashMap<String, ServiceDescription>();
            for (ServiceDescription serviceDescription : servicesToBeDeployed) {
                newServices.put(serviceDescription.getName(), serviceDescription);
            }
            return newServices;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to gather services to be deployed", e);
            }
            return MapUtils.EMPTY_MAP;
        }
    }

    protected void undeployUnnecessary(Map<String, ServiceDescription> newServices) {
        for (OpenLService runningService : ruleService.getServices()) {
            String serviceName = runningService.getName();
            if (!newServices.containsKey(serviceName)) {
                try {
                    ruleService.undeploy(serviceName);
                } catch (RuleServiceUndeployException e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format("Failed to undeploy \"%s\" service", serviceName), e);
                    }
                }
            }
        }
    }

    private boolean isServiceExists(String serviceName) {
        return ruleService.getServiceByName(serviceName) != null;
    }

    protected void redeployExisitng(Map<String, ServiceDescription> newServices) {
        for (ServiceDescription serviceDescription : newServices.values()) {
            if (isServiceExists(serviceDescription.getName())) {
                try {
                    ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
                    ruleService.redeploy(serviceDescription);
                } catch (RuleServiceRedeployException e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format("Failed to redeploy \"%s\" service", serviceDescription.getName()), e);
                    }
                } finally {
                    ServiceDescriptionHolder.getInstance().remove();
                }
            }
        }
    }

    protected void deployNewServices(Map<String, ServiceDescription> newServices) {
        for (ServiceDescription serviceDescription : newServices.values()) {
            if (!isServiceExists(serviceDescription.getName())) {
                try {
                    ServiceDescriptionHolder.getInstance().setServiceDescription(serviceDescription);
                    ruleService.deploy(serviceDescription);
                } catch (RuleServiceDeployException e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format("Failed to deploy \"%s\" service", serviceDescription.getName()), e);
                    }
                } finally {
                    ServiceDescriptionHolder.getInstance().remove();
                }
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
