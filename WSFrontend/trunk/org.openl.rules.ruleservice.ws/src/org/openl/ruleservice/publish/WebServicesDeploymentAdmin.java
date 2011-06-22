package org.openl.ruleservice.publish;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.ServiceDeployException;
import org.openl.rules.ruleservice.publish.IDeploymentAdmin;
import org.springframework.beans.factory.ObjectFactory;

/**
 * DeploymentAdmin to expose services via HTTP.
 * 
 * @author PUdalau
 */
public class WebServicesDeploymentAdmin implements IDeploymentAdmin {

    private static final Log LOG = LogFactory.getLog(WebServicesDeploymentAdmin.class);

    private ObjectFactory<?> serverFactory;
    private Map<OpenLService, Server> runningServices = new HashMap<OpenLService, Server>();
    private String baseAddress;

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String address) {
        this.baseAddress = address;
    }

    public ObjectFactory<?> getServerFactory() {
        return serverFactory;
    }

    public void setServerFactory(ObjectFactory<?> serverFactory) {
        this.serverFactory = serverFactory;
    }

    /* internal for test */ServerFactoryBean getServerFactoryBean() {
        if (serverFactory != null) {
            return (ServerFactoryBean) serverFactory.getObject();
        }
        return new ServerFactoryBean();
    }

    public OpenLService deploy(OpenLService service) throws ServiceDeployException {
        ServerFactoryBean svrFactory = getServerFactoryBean();
        String serviceAddress = baseAddress + service.getUrl();
        svrFactory.setAddress(serviceAddress);
        svrFactory.setServiceClass(service.getServiceClass());
        svrFactory.setServiceBean(service.getServiceBean());
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(svrFactory.getServiceClass().getClassLoader());
        try {
            Server wsServer = svrFactory.create();
            runningServices.put(service, wsServer);
            LOG.info(String.format("Service \"%s\" with URL \"%s\" succesfully deployed.", service.getName(),
                    serviceAddress));
        } catch (Throwable t) {
            throw new ServiceDeployException(String.format("Failed to deploy service \"%s\"", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return service;
    }

    public List<OpenLService> getRunningServices() {
        return new ArrayList<OpenLService>(runningServices.keySet());
    }

    public OpenLService findServiceByName(String name) {
        for (OpenLService service : runningServices.keySet()) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

    public OpenLService undeploy(String serviceName) throws ServiceDeployException {
        OpenLService service = findServiceByName(serviceName);
        if (service == null) {
            throw new ServiceDeployException(String.format("There is no ruuning service with name \"%s\"", serviceName));
        }
        try {
            runningServices.get(service).stop();
            LOG.info(String.format("Service \"%s\" with URL \"%s\" succesfully undeployed.", serviceName, baseAddress
                    + service.getUrl()));
            return service;
        } catch (Throwable t) {
            throw new ServiceDeployException(String.format("Failed to undeploy service \"%s\"", serviceName), t);
        }
    }
}
