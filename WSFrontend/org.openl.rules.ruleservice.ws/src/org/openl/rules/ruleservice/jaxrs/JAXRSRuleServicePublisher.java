package org.openl.rules.ruleservice.jaxrs;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.ext.ExceptionMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.core.ServiceInvocationAdvice;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.publish.URLHelper;
import org.openl.rules.ruleservice.storelogdata.CollectOperationResourceInfoInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectRequestMessageInInterceptor;
import org.openl.rules.ruleservice.storelogdata.CollectResponseMessageOutInterceptor;
import org.openl.rules.ruleservice.storelogdata.PopulateStoreLogDataInterceptor;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataManager;

/**
 * DeploymentAdmin to expose services via HTTP using JAXRS.
 *
 * @author Nail Samatov, Marat Kamalov
 */
public class JAXRSRuleServicePublisher implements RuleServicePublisher {
    public static final String REST_PREFIX = "REST/";

    private final Logger log = LoggerFactory.getLogger(JAXRSRuleServicePublisher.class);

    private final Map<OpenLService, Server> runningServices = new ConcurrentHashMap<>();

    @Value("${ruleservice.authentication.enabled}")
    private boolean authenticationEnabled;

    @Autowired
    @Qualifier("serviceDescriptionInProcess")
    private ObjectFactory<ServiceDescription> serviceDescriptionObjectFactory;

    @Autowired
    private StoreLogDataManager storeLogDataManager;

    @Autowired
    private List<ExceptionMapper> exceptionMappers;

    @Autowired
    private List<Feature> features;

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

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void deploy(final OpenLService service) throws RuleServiceDeployException {
        try {
            if (service.getServiceClass().getMethods().length == 0) {
                throw new Exception("The service has no public methods.");
            }
        } catch (Exception e) {
            throw new RuleServiceDeployException(e.getMessage(), e);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            var svrFactory = new JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean();
            svrFactory.setApplicationContext(service.getServiceContext());
            String url = "/" + getUrl(service);
            svrFactory.setAddress(url);

            svrFactory.getFeatures().addAll(features);

            var serviceObjectMapper = service.getServiceContext().getBean(ServiceInvocationAdvice.OBJECT_MAPPER_ID, ObjectMapper.class);

            svrFactory.setProvider(new TextPlainMessageProvider(serviceObjectMapper));
            svrFactory.setProvider(new JacksonJsonProvider(serviceObjectMapper));

            if (getStoreLogDataManager().isEnabled()) {
                var storeLogDataInInterceptor = new CollectRequestMessageInInterceptor();
                svrFactory.getInInterceptors().add(storeLogDataInInterceptor);
                svrFactory.getInFaultInterceptors().add(storeLogDataInInterceptor);

                var storeLogDataOutInterceptor = new CollectResponseMessageOutInterceptor(getStoreLogDataManager());
                svrFactory.getOutInterceptors().add(storeLogDataOutInterceptor);
                svrFactory.getOutFaultInterceptors().add(storeLogDataOutInterceptor);

                var serviceInterceptor = new PopulateStoreLogDataInterceptor(service, serviceObjectMapper);
                svrFactory.getInInterceptors().add(serviceInterceptor);
                svrFactory.getInFaultInterceptors().add(serviceInterceptor);

                var operationResourceInfoInterceptor = new CollectOperationResourceInfoInterceptor();
                svrFactory.getInInterceptors().add(operationResourceInfoInterceptor);
                svrFactory.getInFaultInterceptors().add(operationResourceInfoInterceptor);
            }

            // Swagger support
            svrFactory.setProviders(exceptionMappers);

            Object proxyServiceBean = new JAXRSOpenLServiceEnhancer().decorateServiceBean(service);
            // The first one is a decorated interface
            Class<?> serviceClass = proxyServiceBean.getClass().getInterfaces()[0];

            var openApiResource = new OpenApiResource(serviceClass, serviceObjectMapper, service, authenticationEnabled);

            svrFactory.setResourceClasses(serviceClass, OpenApiResource.class);
            svrFactory.setResourceProvider(serviceClass, new SingletonResourceProvider(proxyServiceBean));
            svrFactory.setResourceProvider(OpenApiResource.class, new SingletonResourceProvider(openApiResource));

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
        }
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

    @Override
    public String name() {
        return "RESTFUL";
    }
}
