package org.openl.rules.ruleservice.publish;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.springframework.beans.factory.ObjectFactory;

/**
 * DeploymentAdmin to expose services via HTTP.
 * 
 * @author PUdalau, Marat Kamalov
 */
public class WebServicesRuleServicePublisher implements RuleServicePublisher {

    //private final Log log = LogFactory.getLog(WebServicesRuleServicePublisher.class);

    private ObjectFactory<?> serverFactory;
    private Map<OpenLService, ServiceServer> runningServices = new HashMap<OpenLService, ServiceServer>();
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

    public void deploy(OpenLService service) throws RuleServiceDeployException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(service.getServiceClass().getClassLoader());

        ServerFactoryBean svrFactory = getServerFactoryBean();
        String serviceAddress = getBaseAddress() + service.getUrl();
        svrFactory.setAddress(serviceAddress);
        svrFactory.setServiceClass(service.getServiceClass());
        svrFactory.setServiceBean(service.getServiceBean());
        try {
            Server wsServer = svrFactory.create();
            ServiceServer serviceServer = new ServiceServer(wsServer, svrFactory.getDataBinding());
            runningServices.put(service, serviceServer);
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service \"%s\"", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
    
    public DataBinding getDataBinding(String serviceName){
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            return null;
        }
        return runningServices.get(service).getDatabinding();
    }

    public Collection<OpenLService> getServices() {
        return Collections.unmodifiableCollection(runningServices.keySet());
    }

    public OpenLService getServiceByName(String name) {
        for (OpenLService service : runningServices.keySet()) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            throw new RuleServiceUndeployException(String.format("There is no running service with name \"%s\"",
                    serviceName));
        }
        try {
            runningServices.get(service).getServer().destroy();
            runningServices.remove(service);
            service.destroy();
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service \"%s\"", serviceName), t);
        }
    }
    
    public void redeploy(OpenLService service) throws RuleServiceRedeployException {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }

        try {
            undeploy(service.getName());
            deploy(service);
        } catch (RuleServiceDeployException e) {
            throw new RuleServiceRedeployException("Service redeploy was failed", e);
        } catch (RuleServiceUndeployException e) {
            throw new RuleServiceRedeployException("Service redeploy was failed", e);
        }

    }
    
    private static class ServiceServer{
        private Server server;
        private DataBinding databinding;
        
        public ServiceServer(Server server, DataBinding dataBinding) {
            if (server == null) {
                throw new IllegalArgumentException("server arg can't be null!");
            }

            this.server = server;
            this.databinding = dataBinding;
        }
        
        public DataBinding getDatabinding() {
            return databinding;
        }
        
        public Server getServer() {
            return server;
        }
    }
}
