package org.openl.rules.demo.webservice.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.openl.rules.calculation.result.convertor2.CompoundStep;
import org.openl.rules.calculation.result.convertor2.SimpleStep;
import org.openl.rules.demo.AutoPolicyCalculation;
import org.openl.rules.ruleservice.databinding.AegisDatabindingFactoryBean;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class WebServiceTemplate {

    private Client clientInterface;
    private AutoPolicyCalculation staticClientInterface;

    private static final WebServiceTemplate INSTANCE = new WebServiceTemplate();

    private WebServiceTemplate() {
    }

    public static WebServiceTemplate getInstance() {
        return INSTANCE;
    }

    private Client createClientInterface() throws IOException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            JaxWsDynamicClientFactory clientFactory = JaxWsDynamicClientFactory.newInstance();
            return clientFactory.createClient(getAddress() + "?wsdl");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private AutoPolicyCalculation createStaticClientInterface() {
        ClientProxyFactoryBean clientProxyFactoryBean = new ClientProxyFactoryBean();
        clientProxyFactoryBean.setServiceClass(AutoPolicyCalculation.class);
        clientProxyFactoryBean.setWsdlLocation(getAddress() + "?wsdl");
        AegisDatabindingFactoryBean aegisDatabindingFactoryBean = new AegisDatabindingFactoryBean();
        aegisDatabindingFactoryBean.setSupportVariations(true);
        aegisDatabindingFactoryBean.setWriteXsiTypes(true);

        Set<String> overrideTypes = new HashSet<String>();
        overrideTypes.add(SimpleStep.class.getCanonicalName());
        overrideTypes.add(CompoundStep.class.getCanonicalName());
        aegisDatabindingFactoryBean.setOverrideTypes(overrideTypes);

        clientProxyFactoryBean.setDataBinding(aegisDatabindingFactoryBean.createAegisDatabinding());

        return  (AutoPolicyCalculation) clientProxyFactoryBean.create();
    }

    public synchronized Client getClientInterface() throws IOException {
        if (clientInterface == null) {
            return clientInterface = createClientInterface();
        }
        return clientInterface;
    }

    public synchronized AutoPolicyCalculation getStaticClientInterface() {
        if (staticClientInterface == null) {
            return staticClientInterface = createStaticClientInterface();
        }
        return staticClientInterface;
    }

    public String getAddress() {
        Config conf = ConfigFactory.load();
        return conf.getString("ws.address");
    }
}
