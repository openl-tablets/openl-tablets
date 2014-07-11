package org.openl.rules.demo.webservice.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import java.io.IOException;

public class WebServiceTemplate {

    private Client clientInterface;

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

    public synchronized Client getClientInterface() throws IOException {
        if (clientInterface == null) {
            return clientInterface = createClientInterface();
        }
        return clientInterface;
    }

    public String getAddress() {
        Config conf = ConfigFactory.load();
        return conf.getString("ws.address");
    }
}
