package org.openl.rules.ruleservice.publish.test;

import java.util.Collection;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.junit.Before;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.publish.WebServicesRuleServicePublisher;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Class designed for testing rules published via
 * WebServicesRuleServicePublisher.
 * 
 * @author Marat Kamalov
 * 
 */
// @RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration
public class AbstractWebServicesRuleServicePublisherTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    private boolean logEnabled = false;
    private static boolean initialized = false;

    @Before
    public void before() {
        if (!initialized) {
            ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
            serviceManager.start();
            initialized = true;
        }
    }

    protected final boolean isLogEnabled() {
        return logEnabled;
    }

    protected final void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }

    @Override
    public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public final ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected WebServicesRuleServicePublisher getRuleServicePublisher() {
        return getApplicationContext().getBean(WebServicesRuleServicePublisher.class);
    }

    protected Collection<OpenLService> getServices() {
        return getRuleServicePublisher().getServices();
    }

    protected OpenLService getServiceByName(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName arg can't be null");
        }
        return getRuleServicePublisher().getServiceByName(serviceName);
    }

    protected DataBinding getDataBinding() {
        return getApplicationContext().getBean(DataBinding.class);
    }

    protected Object getClient(String serviceName) {
        return getClient(serviceName, (String) null);
    }

    protected Object getClient(String serviceName, String address) {
        return getClient(serviceName, address, null);
    }

    protected <T> T getClient(String serviceName, Class<T> clazz) {
        return (T) getClient(serviceName, null, clazz);
    }

    protected <T> T getClient(String serviceName, String address, Class<T> clazz) {
        return (T) getClient(serviceName, address, clazz, null);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getClient(String serviceName, String address, Class<T> clazz, DataBinding dataBinding) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName arg can't be null");
        }
        OpenLService service = getRuleServicePublisher().getServiceByName(serviceName);
        if (service == null) {
            throw new IllegalStateException(String.format("Service with name=\"%s\"", serviceName));
        }
        DataBinding dataBindingForClient = null;
        Class<?> clazzForClient = null;
        String addressForClient = null;
        if (clazz == null) {
            clazzForClient = service.getServiceClass();
        } else {
            clazzForClient = clazz;
        }
        if (dataBinding == null) {
            dataBindingForClient = getDataBinding();
        } else {
            dataBindingForClient = dataBinding;
        }
        if (address == null) {
            addressForClient = getRuleServicePublisher().getBaseAddress() + service.getUrl();
        } else {
            addressForClient = address;
        }
        ClientProxyFactoryBean clientProxyFactoryBean = new ClientProxyFactoryBean();
        clientProxyFactoryBean.setServiceClass(clazzForClient);
        clientProxyFactoryBean.setAddress(addressForClient);
        clientProxyFactoryBean.setDataBinding(dataBindingForClient);
        ClientFactoryBean clientFactoryBean = new ClientFactoryBean();
        clientFactoryBean.setClientProxyFactoryBean(clientProxyFactoryBean);
        return (T) clientFactoryBean.create();
    }

    protected Client getDynamicClientByServiceName(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName arg can't be null");
        }
        OpenLService service = getRuleServicePublisher().getServiceByName(serviceName);
        if (service == null) {
            throw new IllegalStateException(String.format("Service with name=\"%s\"", serviceName));
        }

        String wsdlLocation = buildWsdlLocation(getRuleServicePublisher().getBaseAddress(), service.getUrl());
        return getDynamicClient(wsdlLocation);
    }

    protected String buildWsdlLocation(String baseUrl, String serviceUrl) {
        return baseUrl + serviceUrl + "?wsdl";
    }

    protected Client getDynamicClient(String wsdlLocation) {
        JaxWsDynamicClientFactory clientFactory = JaxWsDynamicClientFactory.newInstance();
        Client client = clientFactory.createClient(wsdlLocation);
        return client;
    }
}
