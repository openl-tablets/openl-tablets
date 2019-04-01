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
public class RmiRuleServicePublisher extends AbstractRuleServicePublisher implements AvailableServicesPresenter {

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
    protected void deployService(OpenLService service) throws RuleServiceDeployException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            Registry registry = getRegistry();
            String url = URLHelper.processURL(service.getUrl());

            Remote rmiHandler = null;
            if (service.getRmiServiceClass() == null) {
                rmiHandler = enhanceServiceBeanWithDynamicRmiHandler(service);
            } else {
                rmiHandler = enhanceServiceBeanWithStaticRmiHandler(service);
            }
            Remote stub = UnicastRemoteObject.exportObject(rmiHandler, 0);
            registry.bind(url, stub);

            ServiceServer serviceServer = new ServiceServer(url, rmiHandler);
            runningServices.put(service, serviceServer);
            availableServices.add(createServiceInfo(service));
            log.info("Service '{}' has been exposed with URL '{}'.", service.getName(), url);
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
    public OpenLService getServiceByName(String name) {
        for (OpenLService service : runningServices.keySet()) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

    @Override
    protected void undeployService(String serviceName) throws RuleServiceUndeployException {
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
        List<String> methodNames = new ArrayList<>();
        for (Method method : service.getServiceClass().getMethods()) {
            methodNames.add(method.getName());
        }
        Collections.sort(methodNames, (o1, o2) -> o1.compareToIgnoreCase(o2));
        String address = "rmi://" + getRmiHost() + ":" + getRmiPort() + "/" + URLHelper.processURL(service.getUrl());

        return new ServiceInfo(new Date(), service.getName(), methodNames, address, "RMI");
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

        public ServiceServer(String name, Remote rmihandler) {
            if (name == null) {
                throw new IllegalArgumentException("name arg must not be null!");
            }
            if (rmihandler == null) {
                throw new IllegalArgumentException("name rmihandler must not be null!");
            }
            this.name = name;
            this.rmiHandler = rmihandler;
        }

        public String getName() {
            return name;
        }

        public Remote getRmiHandler() {
            return rmiHandler;
        }
    }

    @Override
    public boolean isServiceDeployed(String name) {
        return getServiceByName(name) != null;
    }
}
