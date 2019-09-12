package org.openl.rules.ruleservice.publish;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.logging.CollectObjectSerializerInterceptor;
import org.openl.rules.ruleservice.logging.CollectOpenLServiceInterceptor;
import org.openl.rules.ruleservice.logging.CollectOperationResourceInfoInterceptor;
import org.openl.rules.ruleservice.logging.CollectPublisherTypeInterceptor;
import org.openl.rules.ruleservice.logging.ObjectSerializer;
import org.openl.rules.ruleservice.logging.StoreLoggingFeature;
import org.openl.rules.ruleservice.publish.jaxws.JAXWSEnhancerHelper;
import org.openl.rules.ruleservice.publish.jaxws.JAXWSInvocationHandler;
import org.openl.rules.ruleservice.publish.jaxws.logging.AegisObjectSerializer;
import org.openl.rules.ruleservice.servlet.AvailableServicesPresenter;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * DeploymentAdmin to expose services via HTTP.
 *
 * @author Marat Kamalov
 */
public class JAXWSRuleServicePublisher implements RuleServicePublisher, AvailableServicesPresenter {

    private final Logger log = LoggerFactory.getLogger(JAXWSRuleServicePublisher.class);

    private Map<OpenLService, ServiceServer> runningServices = new HashMap<>();
    private String baseAddress;
    private List<ServiceInfo> availableServices = new ArrayList<>();
    private boolean loggingStoreEnable = false;

    @Autowired
    @Qualifier("webServicesServerPrototype")
    private ObjectFactory<ServerFactoryBean> serverFactoryBeanObjectFactory;

    @Autowired
    private ObjectFactory<StoreLoggingFeature> storeLoggingFeatureObjectFactory;

    public void setLoggingStoreEnable(boolean loggingStoreEnable) {
        this.loggingStoreEnable = loggingStoreEnable;
    }

    public boolean isLoggingStoreEnable() {
        return loggingStoreEnable;
    }

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String address) {
        this.baseAddress = address;
    }

    public ObjectFactory<ServerFactoryBean> getServerFactoryBeanObjectFactory() {
        return serverFactoryBeanObjectFactory;
    }

    public void setServerFactoryBeanObjectFactory(ObjectFactory<ServerFactoryBean> serverFactoryBeanObjectFactory) {
        this.serverFactoryBeanObjectFactory = serverFactoryBeanObjectFactory;
    }

    public ObjectFactory<StoreLoggingFeature> getStoreLoggingFeatureObjectFactory() {
        return storeLoggingFeatureObjectFactory;
    }

    public void setStoreLoggingFeatureObjectFactory(
            ObjectFactory<StoreLoggingFeature> storeLoggingFeatureObjectFactory) {
        this.storeLoggingFeatureObjectFactory = storeLoggingFeatureObjectFactory;
    }

    private ObjectSerializer getObjectSeializer(ServerFactoryBean svrFactory) {
        return new AegisObjectSerializer((AegisDatabinding) svrFactory.getDataBinding());
    }

    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service argument must not be null!");
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            ServerFactoryBean svrFactory = getServerFactoryBeanObjectFactory().getObject();
            ClassLoader origClassLoader = svrFactory.getBus().getExtension(ClassLoader.class);
            try {
                String serviceAddress = getBaseAddress() + URLHelper.processURL(service.getUrl());
                svrFactory.setAddress(serviceAddress);

                Class<?> serviceClass = JAXWSEnhancerHelper.decorateServiceInterface(service);
                svrFactory.setServiceClass(serviceClass);

                Object target = Proxy.newProxyInstance(service.getClassLoader(),
                    new Class<?>[] { service.getServiceClass() },
                    new JAXWSInvocationHandler(service.getServiceBean()));

                svrFactory.setServiceBean(target);

                svrFactory.getBus().setExtension(service.getClassLoader(), ClassLoader.class);
                if (isLoggingStoreEnable()) {
                    svrFactory.getFeatures().add(getStoreLoggingFeatureObjectFactory().getObject());
                    svrFactory.getInInterceptors()
                        .add(new CollectObjectSerializerInterceptor(getObjectSeializer(svrFactory)));
                    svrFactory.getInInterceptors().add(new CollectOpenLServiceInterceptor(service));
                    svrFactory.getInInterceptors()
                        .add(new CollectPublisherTypeInterceptor(RulesDeploy.PublisherType.WEBSERVICE));
                    svrFactory.getInInterceptors().add(new CollectOperationResourceInfoInterceptor());
                    svrFactory.getInFaultInterceptors()
                        .add(new CollectObjectSerializerInterceptor(getObjectSeializer(svrFactory)));
                    svrFactory.getInFaultInterceptors().add(new CollectOpenLServiceInterceptor(service));
                    svrFactory.getInFaultInterceptors()
                        .add(new CollectPublisherTypeInterceptor(RulesDeploy.PublisherType.WEBSERVICE));
                    svrFactory.getInFaultInterceptors().add(new CollectOperationResourceInfoInterceptor());
                }
                Server wsServer = svrFactory.create();

                ServiceServer serviceServer = new ServiceServer(wsServer, svrFactory.getDataBinding());
                runningServices.put(service, serviceServer);
                availableServices.add(createServiceInfo(service));
                log.info("Service '{}' has been exposed with URL '{}'.", service.getName(), serviceAddress);
            } finally {
                svrFactory.getBus().setExtension(origClassLoader, ClassLoader.class);
            }
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public DataBinding getDataBinding(String serviceName) {
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            return null;
        }
        return runningServices.get(service).getDatabinding();
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
            throw new RuleServiceUndeployException(String.format("There is no running service '%s'", serviceName));
        }
        try {
            runningServices.get(service).getServer().destroy();
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

    private ServiceInfo createServiceInfo(OpenLService service) {
        String url = URLHelper.processURL(service.getUrl());
        return new ServiceInfo(new Date(), service.getName(), url, "SOAP", service.getServicePath());
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
        private Server server;
        private DataBinding databinding;

        public ServiceServer(Server server, DataBinding dataBinding) {
            Objects.requireNonNull(server, "server arg must not be null!");
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
