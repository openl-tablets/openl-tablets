package org.openl.rules.ruleservice.publish;

import java.util.*;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSEnhancerHelper;
import org.openl.rules.ruleservice.publish.jaxrs.storelogdata.JacksonObjectSerializer;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.SwaggerStaticFieldsWorkaround;
import org.openl.rules.ruleservice.servlet.AvailableServicesPresenter;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.openl.rules.ruleservice.storelogdata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * DeploymentAdmin to expose services via HTTP using JAXRS.
 *
 * @author Nail Samatov, Marat Kamalov
 */
public class JAXRSRuleServicePublisher implements RuleServicePublisher, AvailableServicesPresenter {
    public static final String REST_PREFIX = "REST/";

    private final Logger log = LoggerFactory.getLogger(JAXRSRuleServicePublisher.class);

    private Map<OpenLService, Server> runningServices = new HashMap<>();
    private String baseAddress;
    private List<ServiceInfo> availableServices = new ArrayList<>();
    private boolean storeLogDataEnabled = false;
    private boolean swaggerPrettyPrint = false;

    @Autowired
    @Qualifier("JAXRSServicesServerPrototype")
    private ObjectFactory<JAXRSServerFactoryBean> serverFactoryBeanObjectFactory;

    @Autowired
    private ObjectFactory<StoreLogDataFeature> storeLoggingFeatureObjectFactory;

    public boolean isStoreLogDataEnabled() {
        return storeLogDataEnabled;
    }

    public void setStoreLogDataEnabled(boolean storeLogDataEnabled) {
        this.storeLogDataEnabled = storeLogDataEnabled;
    }

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    public ObjectFactory<JAXRSServerFactoryBean> getServerFactoryBeanObjectFactory() {
        return serverFactoryBeanObjectFactory;
    }

    public void setServerFactoryBeanObjectFactory(
            ObjectFactory<JAXRSServerFactoryBean> serverFactoryBeanObjectFactory) {
        this.serverFactoryBeanObjectFactory = serverFactoryBeanObjectFactory;
    }

    public ObjectFactory<StoreLogDataFeature> getStoreLoggingFeatureObjectFactory() {
        return storeLoggingFeatureObjectFactory;
    }

    public void setStoreLoggingFeatureObjectFactory(
            ObjectFactory<StoreLogDataFeature> storeLoggingFeatureObjectFactory) {
        this.storeLoggingFeatureObjectFactory = storeLoggingFeatureObjectFactory;
    }

    public void setSwaggerPrettyPrint(boolean swaggerPrettyPrint) {
        this.swaggerPrettyPrint = swaggerPrettyPrint;
    }

    public boolean isSwaggerPrettyPrint() {
        return swaggerPrettyPrint;
    }

    @Override
    public void deploy(final OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service cannot be null");
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            JAXRSServerFactoryBean svrFactory = getServerFactoryBeanObjectFactory().getObject();
            String url = URLHelper.processURL(service.getUrl());
            if (service.getPublishers().size() != 1) {
                url = getBaseAddress() + REST_PREFIX + url;
            } else {
                url = getBaseAddress() + url;
            }
            svrFactory.setAddress(url);
            if (isStoreLogDataEnabled()) {
                svrFactory.getFeatures().add(getStoreLoggingFeatureObjectFactory().getObject());
                svrFactory.getInInterceptors()
                    .add(new CollectObjectSerializerInterceptor(getObjectSerializer(svrFactory)));
                svrFactory.getInInterceptors().add(new CollectOpenLServiceInterceptor(service));
                svrFactory.getInInterceptors()
                    .add(new CollectPublisherTypeInterceptor(RulesDeploy.PublisherType.RESTFUL));
                svrFactory.getInInterceptors().add(new CollectOperationResourceInfoInterceptor());

                svrFactory.getInFaultInterceptors()
                    .add(new CollectObjectSerializerInterceptor(getObjectSerializer(svrFactory)));
                svrFactory.getInFaultInterceptors().add(new CollectOpenLServiceInterceptor(service));
                svrFactory.getInFaultInterceptors()
                    .add(new CollectPublisherTypeInterceptor(RulesDeploy.PublisherType.RESTFUL));
                svrFactory.getInFaultInterceptors().add(new CollectOperationResourceInfoInterceptor());
            }

            Object proxyServiceBean = JAXRSEnhancerHelper.decorateServiceBean(service);
            Class<?> serviceClass = proxyServiceBean.getClass().getInterfaces()[0]; // The first is a decorated
            // interface

            svrFactory.setResourceClasses(serviceClass);

            Swagger2Feature swagger2Feature = getSwagger2Feature(service, serviceClass);
            svrFactory.getFeatures().add(swagger2Feature);

            svrFactory.setResourceProvider(serviceClass, new SingletonResourceProvider(proxyServiceBean));
            ClassLoader origClassLoader = svrFactory.getBus().getExtension(ClassLoader.class);
            try {
                svrFactory.getBus().setExtension(service.getClassLoader(), ClassLoader.class);
                Server wsServer = svrFactory.create();

                runningServices.put(service, wsServer);
                availableServices.add(createServiceInfo(service));
                log.info("Service '{}' has been exposed with URL '{}'.", service.getName(), url);
            } finally {
                svrFactory.getBus().setExtension(origClassLoader, ClassLoader.class);
            }
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'.", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }


    private ObjectSerializer getObjectSerializer(JAXRSServerFactoryBean svrFactory) {
        for (Object provider : svrFactory.getProviders()) {
            if (provider instanceof JacksonJsonProvider) {
                ObjectMapper objectMapper = ((JacksonJsonProvider) provider).locateMapper(null, null);
                return new JacksonObjectSerializer(objectMapper);
            }
        }
        return null;
    }

    private Swagger2Feature getSwagger2Feature(final OpenLService service, Class<?> serviceClass) {
        Swagger2Feature swagger2Feature = new Swagger2Feature();
        swagger2Feature.setRunAsFilter(true);
        swagger2Feature.setScan(false);
        swagger2Feature.setPrettyPrint(isSwaggerPrettyPrint());
        swagger2Feature.setUsePathBasedConfig(true);
        if (serviceClass.getPackage() == null) {
            swagger2Feature.setResourcePackage("default");
        } else {
            swagger2Feature.setResourcePackage(serviceClass.getPackage().getName());
        }
        swagger2Feature.setTitle(service.getName());
        return swagger2Feature;
    }

    @Override
    public Collection<OpenLService> getServices() {
        return new ArrayList<>(runningServices.keySet());
    }

    @Override
    public OpenLService getServiceByName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        for (OpenLService service : runningServices.keySet()) {
            if (service.getName().equals(serviceName)) {
                return service;
            }
        }
        return null;
    }

    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name '%s'.", serviceName));
        }
        try {
            SwaggerStaticFieldsWorkaround.reset();
            runningServices.get(service).destroy();
            runningServices.remove(service);
            removeServiceInfo(serviceName);
            log.info("Service '{}' has been undeployed succesfully.", serviceName);
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service '%s'.", serviceName), t);
        }
    }

    @Override
    public List<ServiceInfo> getAvailableServices() {
        List<ServiceInfo> services = new ArrayList<>(availableServices);
        services.sort(Comparator.comparing(ServiceInfo::getName, String.CASE_INSENSITIVE_ORDER));
        return services;
    }

    private ServiceInfo createServiceInfo(OpenLService service) {
        String url = URLHelper.processURL(service.getUrl());
        if (service.getPublishers().size() != 1) {
            url = REST_PREFIX + url;
        }
        return new ServiceInfo(new Date(), service.getName(), url, "REST", service.getServicePath());
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
}
