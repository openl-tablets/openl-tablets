package org.openl.rules.ruleservice.publish;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.rmi.RmiEnhancerHelper;
import org.openl.rules.ruleservice.rmi.DefaultRmiHandler;
import org.openl.rules.ruleservice.servlet.AvailableServicesPresenter;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeploymentAdmin to expose services via HTTP.
 *
 * @author PUdalau, Marat Kamalov
 */
public class RmiRuleServicePublisher implements RuleServicePublisher, AvailableServicesPresenter {

    private final Logger log = LoggerFactory.getLogger(RmiRuleServicePublisher.class);

    private Map<OpenLService, ServiceServer> runningServices = new HashMap<>();
    private List<ServiceInfo> availableServices = new ArrayList<>();
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
        Objects.requireNonNull(service, "service argument must not be null!");
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            Registry registry = getRegistry();
            String rmiName = service.getRmiName() != null ? service.getRmiName()
                                                          : URLHelper.processURL(service.getUrl());

            Remote rmiHandler = null;
            if (service.getRmiServiceClass() == null) {
                rmiHandler = enhanceServiceBeanWithDynamicRmiHandler(service);
            } else {
                rmiHandler = enhanceServiceBeanWithStaticRmiHandler(service);
            }
            Remote stub = UnicastRemoteObject.exportObject(rmiHandler, 0);
            registry.bind(rmiName, stub);

            ServiceServer serviceServer = new ServiceServer(rmiName, rmiHandler);
            runningServices.put(service, serviceServer);
            availableServices.add(createServiceInfo(service));
            log.info("Service '{}' has been exposed with RMI name '{}'.", service.getName(), rmiName);
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'.", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Collection<OpenLService> getServices() {
        return new ArrayList<>(runningServices.keySet());
    }

    @Override
    public OpenLService getServiceByName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName argument must not be null!");
        for (OpenLService service : runningServices.keySet()) {
            if (service.getName().equals(serviceName)) {
                return service;
            }
        }
        return null;
    }

    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceName, "serviceName argument must not be null!");
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name '%s'.", serviceName));
        }
        try {
            getRegistry().unbind(runningServices.get(service).getName());
            UnicastRemoteObject.unexportObject(runningServices.get(service).getRmiHandler(), true);
            runningServices.remove(service);
            removeServiceInfo(serviceName);
            log.info("Service '{}' has been succesfully undeployed.", serviceName);
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service '%s'.", serviceName), t);
        }
    }

    @Override
    public List<ServiceInfo> getAvailableServices() {
        List<ServiceInfo> services = new ArrayList<>(availableServices);
        Collections.sort(services, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        return services;
    }

    private ServiceInfo createServiceInfo(OpenLService service) throws RuleServiceInstantiationException {
        String address = "rmi://" + getRmiHost() + ":" + getRmiPort() + "/" + URLHelper.processURL(service.getUrl());
        return new ServiceInfo(new Date(), service.getName(), address, "RMI", service.getServicePath());
    }

    private void removeServiceInfo(String serviceName) {
        for (Iterator<ServiceInfo> iterator = availableServices.iterator(); iterator.hasNext();) {
            ServiceInfo serviceInfo = iterator.next();
            if (serviceInfo.getName().equals(serviceName)) {
                iterator.remove();
                break;
            }
        }
    }

    private static class ServiceServer {
        private String name;
        private Remote rmiHandler;

        public ServiceServer(String name, Remote rmiHandler) {
            Objects.requireNonNull(name, "name arg must not be null!");
            Objects.requireNonNull(rmiHandler, "rmiHandler must not be null!");
            this.name = name;
            this.rmiHandler = rmiHandler;
        }

        public String getName() {
            return name;
        }

        public Remote getRmiHandler() {
            return rmiHandler;
        }
    }
}
