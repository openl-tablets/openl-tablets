package org.openl.itest.core;

import java.util.HashSet;
import java.util.Set;

import javax.jws.WebService;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.openl.rules.ruleservice.databinding.AegisDatabindingFactoryBean;

/**
 * @author Vladyslav Pikus, Yury Molchan
 */
public class SoapClientFactory<T> {

    private final String address;
    private final Class<T> service;

    private boolean supportVariations = false;
    private Set<String> overrideTypes = new HashSet<>();

    public SoapClientFactory(String address, Class<T> service) {
        this.address = address;
        this.service = service;
    }

    public SoapClientFactory<T> setSupportVariations(boolean supportVariations) {
        this.supportVariations = supportVariations;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T createProxy() {
        ClientProxyFactoryBean proxyFactory;
        if (service.getAnnotation(WebService.class) != null) {
            // http://cxf.apache.org/docs/a-simple-jax-ws-service.html
            // Simple frontend does not process any JAX-WS annotations!
            proxyFactory = new JaxWsProxyFactoryBean();
        } else {
            // http://cxf.apache.org/docs/simple-frontend.html
            proxyFactory = new ClientProxyFactoryBean();
        }
        proxyFactory.setServiceClass(service);
        proxyFactory.setAddress(address);
        proxyFactory.setDataBinding(createDatabinding());
        return (T) proxyFactory.create();
    }

    private AegisDatabinding createDatabinding() {
        AegisDatabindingFactoryBean databindingFactory = new AegisDatabindingFactoryBean();
        databindingFactory.setSupportVariations(supportVariations);
        databindingFactory.setWriteXsiTypes(true);
        databindingFactory.setOverrideTypes(overrideTypes);
        return databindingFactory.createAegisDatabinding();
    }

}
