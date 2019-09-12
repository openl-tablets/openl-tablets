package org.openl.rules.ruleservice.publish;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.logging.CollectObjectSerializerInterceptor;
import org.openl.rules.ruleservice.logging.CollectOpenLServiceInterceptor;
import org.openl.rules.ruleservice.logging.CollectOperationResourceInfoInterceptor;
import org.openl.rules.ruleservice.logging.CollectPublisherTypeInterceptor;
import org.openl.rules.ruleservice.logging.ObjectSerializer;
import org.openl.rules.ruleservice.logging.StoreLoggingFeature;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSEnhancerHelper;
import org.openl.rules.ruleservice.publish.jaxrs.WadlGenerator;
import org.openl.rules.ruleservice.publish.jaxrs.logging.JacksonObjectSerializer;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.SwaggerStaticFieldsWorkaround;
import org.openl.rules.ruleservice.servlet.AvailableServicesPresenter;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
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
    private boolean loggingStoreEnable = false;
    private boolean swaggerPrettyPrint = false;

    @Autowired
    private ObjectFactory<WadlGenerator> wadlGeneratorObjectFactory;

    @Autowired
    @Qualifier("JAXRSServicesServerPrototype")
    private ObjectFactory<JAXRSServerFactoryBean> serverFactoryBeanObjectFactory;

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

    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    public ObjectFactory<WadlGenerator> getWadlGeneratorObjectFactory() {
        return wadlGeneratorObjectFactory;
    }

    public void setWadlGeneratorObjectFactory(ObjectFactory<WadlGenerator> wadlGeneratorObjectFactory) {
        this.wadlGeneratorObjectFactory = wadlGeneratorObjectFactory;
    }

    public ObjectFactory<JAXRSServerFactoryBean> getServerFactoryBeanObjectFactory() {
        return serverFactoryBeanObjectFactory;
    }

    public void setServerFactoryBeanObjectFactory(
            ObjectFactory<JAXRSServerFactoryBean> serverFactoryBeanObjectFactory) {
        this.serverFactoryBeanObjectFactory = serverFactoryBeanObjectFactory;
    }

    public ObjectFactory<StoreLoggingFeature> getStoreLoggingFeatureObjectFactory() {
        return storeLoggingFeatureObjectFactory;
    }

    public void setStoreLoggingFeatureObjectFactory(
            ObjectFactory<StoreLoggingFeature> storeLoggingFeatureObjectFactory) {
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
        Objects.requireNonNull(service, "service argument must not be null!");
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
            if (isLoggingStoreEnable()) {
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

                setWADLGenerator(svrFactory, wsServer);

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

    private void setWADLGenerator(JAXRSServerFactoryBean svrFactory, Server wsServer) throws NoSuchFieldException,
                                                                                      IllegalAccessException {
        ServerProviderFactory serverProviderFactory = (ServerProviderFactory) wsServer.getEndpoint()
            .get(ServerProviderFactory.class.getName());
        Field wadlGeneratorField = ServerProviderFactory.class.getDeclaredField("wadlGenerator");
        wadlGeneratorField.setAccessible(true);
        wadlGeneratorField.set(serverProviderFactory,
            new ProviderInfo<ContainerRequestFilter>(
                (ContainerRequestFilter) getWadlGeneratorObjectFactory().getObject(),
                svrFactory.getBus(),
                true));
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
                String.format("There is no running service with name '%s'", serviceName));
        }
        try {
            SwaggerStaticFieldsWorkaround.reset();
            runningServices.get(service).destroy();
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
