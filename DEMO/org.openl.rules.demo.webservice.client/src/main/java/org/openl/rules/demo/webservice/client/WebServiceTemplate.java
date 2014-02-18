package org.openl.rules.demo.webservice.client;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class WebServiceTemplate {

    private Client clientInterface;

	private static final WebServiceTemplate INSTANCE = new WebServiceTemplate();

	private WebServiceTemplate(){}

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

	public String getAddress() throws IOException {
		Properties properties = new Properties();
		InputStream stream = getClass().getResourceAsStream("/ws.properties");
		try {
			properties.load(stream);
			return properties.getProperty("ws.address");
		} finally {
			stream.close();
		}
	}

}
