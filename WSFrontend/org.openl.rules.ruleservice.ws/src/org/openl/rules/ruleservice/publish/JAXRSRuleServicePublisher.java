package org.openl.rules.ruleservice.publish;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSOpenLServiceEnhancer;
import org.openl.rules.ruleservice.publish.jaxrs.storelogdata.JacksonObjectSerializer;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiHackContainerRequestFilter;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiHackContainerResponseFilter;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiObjectMapperHack;
import org.openl.rules.ruleservice.storelogdata.CollectObjectSerializerInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectOpenLServiceInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectOperationResourceInfoInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectPublisherTypeInterceptor;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataFeature;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataManager;
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
public class JAXRSRuleServicePublisher implements RuleServicePublisher {
    public static final String REST_PREFIX = "REST/";

    private final Logger log = LoggerFactory.getLogger(JAXRSRuleServicePublisher.class);

    private final Map<OpenLService, Server> runningServices = new ConcurrentHashMap<>();

    private boolean swaggerPrettyPrint = false;

    // false is for testing purposes, see org.openl.rules.ruleservice.servlet.SpringInitializer
    @Autowired(required = false)
    private String servletContextPath;

    @Autowired
    private ObjectFactory<JAXRSOpenLServiceEnhancer> serviceEnhancerObjectFactory;

    @Autowired
    @Qualifier("jaxrsServiceServerPrototype")
    private ObjectFactory<JAXRSServerFactoryBean> serverFactoryBeanObjectFactory;

    @Autowired
    private ObjectFactory<StoreLogDataFeature> storeLoggingFeatureObjectFactory;

    @Autowired
    @Qualifier("jaxrsOpenApiObjectMapper")
    private ObjectFactory<ObjectMapper> jaxrsOpenApiObjectMapper;

    @Autowired
    @Qualifier("serviceDescriptionInProcess")
    private ObjectFactory<ServiceDescription> serviceDescriptionObjectFactory;

    @Autowired
    private StoreLogDataManager storeLogDataManager;

    public StoreLogDataManager getStoreLogDataManager() {
        return storeLogDataManager;
    }

    public void setStoreLogDataManager(StoreLogDataManager storeLogDataManager) {
        this.storeLogDataManager = storeLogDataManager;
    }

    public ObjectFactory<ServiceDescription> getServiceDescriptionObjectFactory() {
        return serviceDescriptionObjectFactory;
    }

    public void setServiceDescriptionObjectFactory(ObjectFactory<ServiceDescription> serviceDescriptionObjectFactory) {
        this.serviceDescriptionObjectFactory = serviceDescriptionObjectFactory;
    }

    public ObjectFactory<ObjectMapper> getJaxrsOpenApiObjectMapper() {
        return jaxrsOpenApiObjectMapper;
    }

    public void setJaxrsOpenApiObjectMapper(ObjectFactory<ObjectMapper> jaxrsOpenApiObjectMapper) {
        this.jaxrsOpenApiObjectMapper = jaxrsOpenApiObjectMapper;
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

    public ObjectFactory<JAXRSOpenLServiceEnhancer> getServiceEnhancerObjectFactory() {
        return serviceEnhancerObjectFactory;
    }

    public void setServiceEnhancerObjectFactory(ObjectFactory<JAXRSOpenLServiceEnhancer> serviceEnhancerObjectFactory) {
        this.serviceEnhancerObjectFactory = serviceEnhancerObjectFactory;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void deploy(final OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service cannot be null");
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        OpenApiObjectMapperHack openApiObjectMapperHack = null;
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            JAXRSServerFactoryBean svrFactory = getServerFactoryBeanObjectFactory().getObject();
            String url = "/" + getUrl(service);
            svrFactory.setAddress(url);
            if (getStoreLogDataManager().isEnabled()) {
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

            // Swagger support
            ObjectMapper openApiObjectMapper;
            openApiObjectMapperHack = new OpenApiObjectMapperHack();
            openApiObjectMapperHack.apply(openApiObjectMapper = getJaxrsOpenApiObjectMapper().getObject());
            ((List) svrFactory.getProviders()).add(new OpenApiHackContainerRequestFilter(openApiObjectMapper));
            ((List) svrFactory.getProviders()).add(new OpenApiHackContainerResponseFilter());

            JAXRSOpenLServiceEnhancer jaxrsOpenLServiceEnhancer = getServiceEnhancerObjectFactory().getObject();
            Object proxyServiceBean = jaxrsOpenLServiceEnhancer.decorateServiceBean(service,  openApiObjectMapper, servletContextPath + url);
            // The first one is a decorated interface
            Class<?> serviceClass = proxyServiceBean.getClass().getInterfaces()[0];
            svrFactory.setResourceClasses(serviceClass);

            OpenApiFeature openApiFeature = getOpenAPIv3Feature(serviceClass);
            svrFactory.getFeatures().add(openApiFeature);

            svrFactory.setResourceProvider(serviceClass, new SingletonResourceProvider(proxyServiceBean));
            ClassLoader origClassLoader = svrFactory.getBus().getExtension(ClassLoader.class);
            try {
                svrFactory.getBus().setExtension(service.getClassLoader(), ClassLoader.class);
                Server wsServer = svrFactory.create();
                runningServices.put(service, wsServer);
                log.info("Service '{}' has been exposed with URL '{}'.", service.getDeployPath(), url);
            } finally {
                svrFactory.getBus().setExtension(origClassLoader, ClassLoader.class);
            }
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'.", service.getDeployPath()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            if (openApiObjectMapperHack != null) {
                openApiObjectMapperHack.revert();
            }
        }
    }

    private ObjectSerializer getObjectSerializer(JAXRSServerFactoryBean svrFactory) {
        ObjectMapper objectMapper = getObjectMapper(svrFactory);
        return objectMapper == null ? null : new JacksonObjectSerializer(objectMapper);
    }

    private ObjectMapper getObjectMapper(JAXRSServerFactoryBean svrFactory) {
        for (Object provider : svrFactory.getProviders()) {
            if (provider instanceof JacksonJsonProvider) {
                return ((JacksonJsonProvider) provider).locateMapper(null, null);
            }
        }
        return null;
    }

    private OpenApiFeature getOpenAPIv3Feature(final Class<?> serviceClass) {
        final OpenApiFeature openApiFeature = new OpenApiFeature();
        openApiFeature.setRunAsFilter(false);
        openApiFeature.setScan(false);
        openApiFeature.setPrettyPrint(isSwaggerPrettyPrint());
        openApiFeature.setScanKnownConfigLocations(false);
        openApiFeature.setUseContextBasedConfig(false);
        openApiFeature.setScannerClass(io.swagger.v3.jaxrs2.integration.JaxrsApplicationScanner.class.getName());
        openApiFeature.setResourcePackages(Collections.singleton(serviceClass.getPackage().getName()));
        return openApiFeature;
    }

    @Override
    public OpenLService getServiceByDeploy(String deployPath) {
        Objects.requireNonNull(deployPath, "deployPath cannot be null");
        for (OpenLService service : runningServices.keySet()) {
            if (Objects.equals(service.getDeployPath(), deployPath)) {
                return service;
            }
        }
        return null;
    }

    @Override
    public void undeploy(OpenLService service) throws RuleServiceUndeployException {
        Objects.requireNonNull(service, "service cannot be null");
        Server server = runningServices.get(service);
        if (server == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name '%s'.", service.getDeployPath()));
        }
        try {
            server.destroy();
            //TODO
            runningServices.remove(service);
            log.info("Service '{}' has been undeployed successfully.", service.getDeployPath());
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service '%s'.", service.getDeployPath()),
                t);
        }
    }

    @Override
    public String getUrl(OpenLService service) {
        String url = URLHelper.processURL(service.getUrl());
        int numOfServicesWithUrls = service.getPublishers().size();
        if (service.getPublishers().contains(RulesDeploy.PublisherType.KAFKA.toString())) {
            numOfServicesWithUrls--;
        }
        if (service.getPublishers().contains(RulesDeploy.PublisherType.RMI.toString())) {
            numOfServicesWithUrls--;
        }
        if (numOfServicesWithUrls != 1) {
            url = REST_PREFIX + url;
        }
        return url;
    }
}
