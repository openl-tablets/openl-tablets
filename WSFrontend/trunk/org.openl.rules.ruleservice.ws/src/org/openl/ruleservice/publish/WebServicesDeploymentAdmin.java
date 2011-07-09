package org.openl.ruleservice.publish;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
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
    private AbstractDataBinding dataBinding;
    private Map<OpenLService, Server> runningServices = new HashMap<OpenLService, Server>();
    private String baseAddress;

    public void setDataBinding(AbstractDataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }
    
    public AbstractDataBinding getDataBinding() {
        return dataBinding;
    }
    
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

    private ReflectionServiceFactoryBean getServiceFactory(OpenLService service){
        ReflectionServiceFactoryBean serviceFactory = new ReflectionServiceFactoryBean();
        //serviceFactory.setServiceClass(service.getServiceBean().getClass());
        serviceFactory.setDataBinding(getDataBinding());
        serviceFactory.setWrapped(false);
        return serviceFactory;
    }
    
    public OpenLService deploy(OpenLService service) throws ServiceDeployException {
        ServerFactoryBean svrFactory = getServerFactoryBean();
        ReflectionServiceFactoryBean serviceFactory = getServiceFactory(service);
        svrFactory.setServiceFactory(serviceFactory);
        String serviceAddress = baseAddress + service.getUrl();
        svrFactory.setAddress(serviceAddress);
        svrFactory.setServiceClass(service.getServiceClass());
        svrFactory.setServiceBean(service.getServiceBean());
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(service.getServiceClass().getClassLoader());
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
            runningServices.remove(service);
            return service;
        } catch (Throwable t) {
            throw new ServiceDeployException(String.format("Failed to undeploy service \"%s\"", serviceName), t);
        }
    }
}
