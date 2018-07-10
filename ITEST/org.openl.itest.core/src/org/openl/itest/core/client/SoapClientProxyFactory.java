package org.openl.itest.core.client;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.openl.rules.ruleservice.databinding.AegisDatabindingFactoryBean;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vladyslav Pikus
 */
public class SoapClientProxyFactory<T> {

    private final String address;
    private final Class<T> service;

    private boolean supportVariations = false;
    private Set<String> overrideTypes = new HashSet<>();

    public SoapClientProxyFactory(String address, Class<T> service) {
        this.address = address;
        this.service = service;
    }

    public SoapClientProxyFactory<T> setSupportVariations(boolean supportVariations) {
        this.supportVariations = supportVariations;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T createProxy() {
        ClientProxyFactoryBean proxyFactory = new ClientProxyFactoryBean();
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
