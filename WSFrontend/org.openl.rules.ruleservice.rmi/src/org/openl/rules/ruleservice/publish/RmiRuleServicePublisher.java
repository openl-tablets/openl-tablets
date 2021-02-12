package org.openl.rules.ruleservice.publish;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.rmi.RmiEnhancerHelper;
import org.openl.rules.ruleservice.rmi.DefaultRmiHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeploymentAdmin to expose services via HTTP.
 *
 * @author PUdalau, Marat Kamalov
 */
public class RmiRuleServicePublisher implements RuleServicePublisher {

    private final Logger log = LoggerFactory.getLogger(RmiRuleServicePublisher.class);

    private final Map<OpenLService, ServiceServer> runningServices = new HashMap<>();
    private int rmiPort = 1099; // Default RMI port
    private String rmiHost = "127.0.0.1"; // Default RMI host

    public void setRmiPort(int rmiPort) {
        this.rmiPort = rmiPort;
    }

    public int getRmiPort() {
        return rmiPort;
    }

    public String getRmiHost() {
        return rmiHost;
    }

    public void setRmiHost(String rmiHost) {
        this.rmiHost = rmiHost;
    }

    public synchronized Registry getRegistry() throws RemoteException {
        synchronized (LocateRegistry.class) {
            try {
                // Retrieve existing registry.
                Registry reg = LocateRegistry.getRegistry(getRmiPort());
                reg.list();
                return reg;
            } catch (RemoteException ex) {
                // Assume no registry found -> create new one.
                return LocateRegistry.createRegistry(getRmiPort());
            }
        }
    }

    protected DefaultRmiHandler enhanceServiceBeanWithDynamicRmiHandler(OpenLService service) throws Exception {
        return RmiEnhancerHelper.decorateBeanWithDynamicRmiHandler(service.getServiceBean(), service);
    }

    protected Remote enhanceServiceBeanWithStaticRmiHandler(OpenLService service) throws Exception {
        return RmiEnhancerHelper.decorateBeanWithStaticRmiHandler(service.getServiceBean(), service);
    }

    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service cannot be null");
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            Registry registry = getRegistry();
            String rmiName = service.getRmiName() != null ? service.getRmiName()
                                                          : URLHelper.processURL(service.getUrl());

            Remote rmiHandler;
            if (service.getRmiServiceClass() == null) {
                rmiHandler = enhanceServiceBeanWithDynamicRmiHandler(service);
            } else {
                rmiHandler = enhanceServiceBeanWithStaticRmiHandler(service);
            }
            Remote stub = UnicastRemoteObject.exportObject(rmiHandler, 0);
            registry.bind(rmiName, stub);

            ServiceServer serviceServer = new ServiceServer(rmiName, rmiHandler);
            runningServices.put(service, serviceServer);
            log.info("Service '{}' has been exposed with RMI deploy path '{}'.", service.getDeployPath(), rmiName);
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'.", service.getDeployPath()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public OpenLService getServiceByDeploy(String deployPath) {
        Objects.requireNonNull(deployPath, "deployPath cannot be null");
        for (OpenLService service : runningServices.keySet()) {
            if (service.getDeployPath().equals(deployPath)) {
                return service;
            }
        }
        return null;
    }

    @Override
    public void undeploy(OpenLService service) throws RuleServiceUndeployException {
        Objects.requireNonNull(service, "service cannot be null");
        ServiceServer server = runningServices.get(service);
        if (server == null) {
            throw new RuleServiceUndeployException(
                    String.format("There is no running service with deploy path '%s'.", service.getDeployPath()));
        }
        try {
            getRegistry().unbind(server.getName());
            UnicastRemoteObject.unexportObject(server.getRmiHandler(), true);
            runningServices.remove(service);
            log.info("Service '{}' has been undeployed succesfully.", service.getDeployPath());
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service '%s'.", service.getDeployPath()), t);
        }
    }

    @Override
    public String getUrl(OpenLService service) {
        return "rmi://" + getRmiHost() + ":" + getRmiPort() + "/" + URLHelper.processURL(service.getUrl());
    }

    private static class ServiceServer {
        private final String name;
        private final Remote rmiHandler;

        public ServiceServer(String name, Remote rmiHandler) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.rmiHandler = Objects.requireNonNull(rmiHandler, "rmiHandler cannot be null");
        }

        public String getName() {
            return name;
        }

        public Remote getRmiHandler() {
            return rmiHandler;
        }
    }
}
