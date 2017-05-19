package org.openl.rules.demo.webservice.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

@ManagedBean(name = "client")
@SessionScoped
public class WSClient {

    private Client clientInterface;

    public WSClient() throws MalformedURLException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            ExternalContext externalContext = ctx.getExternalContext();
            String scheme = externalContext.getRequestScheme();
            String host = externalContext.getRequestServerName();
            int port = externalContext.getRequestServerPort();
            String path = externalContext.getInitParameter("ws.path");
            URL url = new URL(scheme, host, port, path);
            JaxWsDynamicClientFactory clientFactory = JaxWsDynamicClientFactory.newInstance();
            clientInterface = clientFactory.createClient(url);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public Object invoke(String method, Object... params) throws Exception {
        return clientInterface.invoke(method, params)[0];
    }
}
