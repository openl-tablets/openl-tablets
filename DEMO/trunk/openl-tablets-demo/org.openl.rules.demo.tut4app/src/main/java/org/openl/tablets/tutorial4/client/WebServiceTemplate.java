package org.openl.tablets.tutorial4.client;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.openl.tablets.tutorial4.Tutorial_4WrapperInterface;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class WebServiceTemplate {
	private Tutorial_4WrapperInterface clientInterface;

	private static final WebServiceTemplate INSTANCE = new WebServiceTemplate();

	private WebServiceTemplate(){}

	public static WebServiceTemplate getInstance() {
		return INSTANCE;
	}

    private Tutorial_4WrapperInterface createClientInterface() throws IOException {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setServiceClass(Tutorial_4WrapperInterface.class);
        factory.setAddress(getAddress());
        factory.setDataBinding(new AegisDatabinding());
        return (Tutorial_4WrapperInterface) factory.create();
    }

	private synchronized Tutorial_4WrapperInterface getClientInterface() throws IOException {
		if (clientInterface == null) {
			return clientInterface = createClientInterface();
		}
		return clientInterface;
	}

	private String getAddress() throws IOException {
		Properties properties = new Properties();
		InputStream stream = getClass().getResourceAsStream("/ws.properties");
		try {
			properties.load(stream);
			return properties.getProperty("ws.address");
		} finally {
			stream.close();
		}
	}

	public Object run(WebServiceCallback callback) throws Exception {
		return callback.doAction(getClientInterface());
	}
}
