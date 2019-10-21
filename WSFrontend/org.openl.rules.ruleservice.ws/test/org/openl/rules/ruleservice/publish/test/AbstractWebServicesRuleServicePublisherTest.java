package org.openl.rules.ruleservice.publish.test;

import java.util.Collection;
import java.util.Objects;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.junit.Before;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.publish.JAXWSRuleServicePublisher;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Class designed for testing rules published via WebServicesRuleServicePublisher.
 *
 * @author Marat Kamalov
 *
 */
// @RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration
public class AbstractWebServicesRuleServicePublisherTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    private static boolean initialized = false;

    @Before
    public void before() {
        if (!initialized) {
            ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
            serviceManager.start();
            initialized = true;
        }
    }

    @Override
    public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Returns ApplicationContext.
     *
     * @return application context
     */
    public final ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Returns WebServicesRuleServicePublisher.
     *
     * @return WebServicesRuleServicePublisher
     */
    protected JAXWSRuleServicePublisher getRuleServicePublisher() {
        return getApplicationContext().getBean(JAXWSRuleServicePublisher.class);
    }

    /**
     * Returns all deployed services.
     *
     * @return a collection of deployed services
     */
    protected Collection<OpenLService> getServices() {
        return getRuleServicePublisher().getServices();
    }

    /**
     * Returns deployed service by name.
     *
     * @param serviceName deployed service name
     * @return service
     * @throws ServiceNotFoundException occurs if service with specified name not deployed
     */
    protected OpenLService getServiceByName(String serviceName) throws ServiceNotFoundException {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        OpenLService service = getRuleServicePublisher().getServiceByName(serviceName);
        if (service == null) {
            throw new ServiceNotFoundException(String.format("Service '%' is not found.", serviceName));
        }
        return service;
    }

    /**
     * Return dataBinding. This data binding is used in tests by default.
     *
     * @param serviceName - serviceName
     *
     * @return data binding
     */
    protected DataBinding getDataBinding(String serviceName) {
        return getRuleServicePublisher().getDataBinding(serviceName);
    }

    /**
     * Creates and returns client for specified service.
     *
     * @param serviceName deployed service name
     * @return client object
     * @throws ServiceNotFoundException occurs if service with specified name not deployed
     */
    protected Object getClient(String serviceName) throws ServiceNotFoundException {
        return getClient(serviceName, (String) null);
    }

    /**
     * Creates and returns client for specified service address by deployed service name
     *
     * @param serviceName deployed service name
     * @param address address
     * @return client
     * @throws ServiceNotFoundException occurs if service with specified name not deployed
     */
    protected Object getClient(String serviceName, String address) throws ServiceNotFoundException {
        return getClient(serviceName, address, null);
    }

    /**
     * Creates and returns client by deployed service name. Result object casts to defined type.
     *
     * @param serviceName deployed service name
     * @param clazz service type
     * @return client
     * @throws ServiceNotFoundException occurs if service with specified name not deployed
     */
    protected <T> T getClient(String serviceName, Class<T> clazz) throws ServiceNotFoundException {
        return getClient(serviceName, null, clazz);
    }

    /**
     * Creates and returns client with specified address and type by deployed service name
     *
     * @param serviceName deployed service name
     * @param address service address
     * @param clazz service type
     * @return client
     * @throws ServiceNotFoundException occurs if service with specified name not deployed
     */
    protected <T> T getClient(String serviceName, String address, Class<T> clazz) throws ServiceNotFoundException {
        return getClient(serviceName, address, clazz, null);
    }

    /**
     * Creates and returns client with specified address, data binding and type by deployed service name.
     *
     * @param serviceName deployed service name
     * @param address service address
     * @param clazz service type
     * @param dataBinding databiding
     * @return client
     * @throws ServiceNotFoundException
     */
    @SuppressWarnings("unchecked")
    protected <T> T getClient(String serviceName,
            String address,
            Class<T> clazz,
            DataBinding dataBinding) throws ServiceNotFoundException {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        OpenLService service = getServiceByName(serviceName);
        DataBinding dataBindingForClient = null;
        Class<?> clazzForClient = null;
        String addressForClient = null;
        if (clazz == null) {
            try {
                clazzForClient = service.getServiceClass();
            } catch (RuleServiceInstantiationException e) {
                throw new ServiceNotFoundException(String.format("Service '%' is not found.", serviceName));
            }
        } else {
            clazzForClient = clazz;
        }
        if (dataBinding == null) {
            dataBindingForClient = getDataBinding(serviceName);
        }
        if (address == null) {
            addressForClient = getRuleServicePublisher().getBaseAddress() + service.getUrl();
        } else {
            addressForClient = address;
        }
        ClientProxyFactoryBean clientProxyFactoryBean = new ClientProxyFactoryBean();
        clientProxyFactoryBean.setServiceClass(clazzForClient);
        clientProxyFactoryBean.setAddress(addressForClient);
        if (dataBindingForClient != null) {
            clientProxyFactoryBean.setDataBinding(dataBindingForClient);
        }
        ClientFactoryBean clientFactoryBean = new ClientFactoryBean();
        clientFactoryBean.setClientProxyFactoryBean(clientProxyFactoryBean);
        return (T) clientFactoryBean.create();
    }

    /**
     * Creates and returns dynamic client for service by deployed service name
     *
     * @param serviceName service name
     * @return dynamic client
     * @throws ServiceNotFoundException occurs if service with specified name not deployed
     */
    protected Client getDynamicClientByServiceName(String serviceName) throws ServiceNotFoundException {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        OpenLService service = getServiceByName(serviceName);

        String wsdlLocation = buildWsdlLocation(getRuleServicePublisher().getBaseAddress(), service.getUrl());
        return getDynamicClient(wsdlLocation);
    }

    protected String buildWsdlLocation(String baseUrl, String serviceUrl) {
        return baseUrl + serviceUrl + "?wsdl";
    }

    /**
     * Creates and returns service
     *
     * @param wsdlLocation wsdl location url
     * @return
     */
    protected Client getDynamicClient(String wsdlLocation) {
        Objects.requireNonNull(wsdlLocation, "wsdlLocation cannot be null");
        JaxWsDynamicClientFactory clientFactory = JaxWsDynamicClientFactory.newInstance();
        Client client = clientFactory.createClient(wsdlLocation);
        return client;
    }
}
