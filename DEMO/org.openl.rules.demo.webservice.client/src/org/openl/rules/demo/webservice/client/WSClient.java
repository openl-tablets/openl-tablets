package org.openl.rules.demo.webservice.client;

import java.net.URI;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

@ManagedBean(name = "client")
@SessionScoped
public class WSClient {

    private WebTarget target;

    public WSClient() throws Exception {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            ExternalContext externalContext = ctx.getExternalContext();
            String scheme = externalContext.getRequestScheme();
            String host = externalContext.getRequestServerName();
            int port = externalContext.getRequestServerPort();
            String path = externalContext.getInitParameter("ws.path");
            URI uri = new URI(scheme, null, host, port, path, null, null);

            Client client = ClientBuilder.newClient();
            target = client.target(uri);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public Object invoke(String method, String json) throws Exception {
        WebTarget path = target.path(method);
        Invocation.Builder request = path.request();
        Object response;
        if (json == null) {
            response = request.get(String.class);
        } else {
            response = request.post(Entity.json(json), String.class);
        }
        return response;
    }
}
