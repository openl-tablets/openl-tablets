package org.openl.rules.ruleservice.publish;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSOpenLServiceEnhancer;
import org.openl.rules.ruleservice.publish.jaxrs.storelogdata.JacksonObjectSerializer;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.SwaggerHackContainerRequestFilter;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.SwaggerHackContainerResponseFilter;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.SwaggerObjectMapperHack;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.SwaggerStaticFieldsWorkaround;
import org.openl.rules.ruleservice.storelogdata.CollectObjectSerializerInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectOpenLServiceInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectOperationResourceInfoInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectPublisherTypeInterceptor;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataFeature;
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

    private Map<OpenLService, Server> runningServices = new HashMap<>();
    private boolean storeLogDataEnabled = false;
    private boolean swaggerPrettyPrint = false;

    @Autowired
    private ObjectFactory<JAXRSOpenLServiceEnhancer> serviceEnhancerObjectFactory;

    @Autowired
    @Qualifier("jaxrsServiceServerPrototype")
    private ObjectFactory<JAXRSServerFactoryBean> serverFactoryBeanObjectFactory;

    @Autowired
    private ObjectFactory<StoreLogDataFeature> storeLoggingFeatureObjectFactory;

    public boolean isStoreLogDataEnabled() {
        return storeLogDataEnabled;
    }

    public void setStoreLogDataEnabled(boolean storeLogDataEnabled) {
        this.storeLogDataEnabled = storeLogDataEnabled;
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
        SwaggerObjectMapperHack swaggerObjectMapperHack = null;
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            JAXRSServerFactoryBean svrFactory = getServerFactoryBeanObjectFactory().getObject();
            String url = "/" + getUrl(service);
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

            JAXRSOpenLServiceEnhancer jaxrsOpenLServiceEnhancer = getServiceEnhancerObjectFactory().getObject();
            Object proxyServiceBean = jaxrsOpenLServiceEnhancer.decorateServiceBean(service);
            Class<?> serviceClass = proxyServiceBean.getClass().getInterfaces()[0]; // The first is a decorated
            // interface

            svrFactory.setResourceClasses(serviceClass);

            // Swagger support
            swaggerObjectMapperHack = new SwaggerObjectMapperHack();
            swaggerObjectMapperHack.apply(getObjectMapper(svrFactory));
            ((List) svrFactory.getProviders()).add(new SwaggerHackContainerRequestFilter(getObjectMapper(svrFactory)));
            ((List) svrFactory.getProviders()).add(new SwaggerHackContainerResponseFilter());
            Swagger2Feature swagger2Feature = getSwagger2Feature(service, serviceClass);
            svrFactory.getFeatures().add(swagger2Feature);

            svrFactory.setResourceProvider(serviceClass, new SingletonResourceProvider(proxyServiceBean));
            ClassLoader origClassLoader = svrFactory.getBus().getExtension(ClassLoader.class);
            try {
                svrFactory.getBus().setExtension(service.getClassLoader(), ClassLoader.class);
                Server wsServer = svrFactory.create();

                runningServices.put(service, wsServer);
                log.info("Service '{}' has been exposed with URL '{}'.", service.getName(), url);
            } finally {
                svrFactory.getBus().setExtension(origClassLoader, ClassLoader.class);
            }
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'.", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            if (swaggerObjectMapperHack != null) {
                try {
                    swaggerObjectMapperHack.revert();
                } catch (Exception e) {
                    log.debug(e.getMessage(), e);
                }
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
    public void undeploy(OpenLService service) throws RuleServiceUndeployException {
        Objects.requireNonNull(service, "service cannot be null");
        Server server = runningServices.get(service);
        if (server == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name '%s'.", service.getName()));
        }
        try {
            SwaggerStaticFieldsWorkaround.reset();
            server.destroy();
            runningServices.remove(service);
            log.info("Service '{}' has been undeployed successfully.", service.getName());
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service '%s'.", service.getName()),
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
