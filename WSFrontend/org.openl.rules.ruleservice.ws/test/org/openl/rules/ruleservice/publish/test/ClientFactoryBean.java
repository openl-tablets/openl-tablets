package org.openl.rules.ruleservice.publish.test;

import java.util.Objects;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.FactoryBean;

/**
 * Creates rating service client proxy object. Designed as Spring factory bean. Uses CXF ClientProxyFactoryBean for
 * building client object.
 *
 * Set "logging" property to true for logging enable.
 *
 * @author Marat Kamalov
 *
 */
final class ClientFactoryBean implements FactoryBean<Object> {
    private static final long TIME_OUT = 60000;

    private ClientProxyFactoryBean clientProxyFactoryBean;

    private boolean logging = false;

    private Long timeOut;

    public boolean isLogging() {
        return logging;
    }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }

    public Long getTimeOut() {
        if (timeOut != null) {
            return timeOut;
        }
        return TIME_OUT;
    }

    public void setTimeOut(Long timeOut) {
        this.timeOut = Objects.requireNonNull(timeOut, "timeOut cannot be null");
    }

    public ClientProxyFactoryBean getClientProxyFactoryBean() {
        return clientProxyFactoryBean;
    }

    public void setClientProxyFactoryBean(ClientProxyFactoryBean clientProxyFactoryBean) {
        this.clientProxyFactoryBean = Objects.requireNonNull(clientProxyFactoryBean,
            "clientProxyFactoryBean cannot be null");
    }

    @Override
    public Class<?> getObjectType() {
        if (getClientProxyFactoryBean() == null) {
            throw new IllegalStateException("clientProxyFactoryBean cannot be null");
        }
        if (getClientProxyFactoryBean().getServiceClass() == null) {
            throw new IllegalStateException("clientProxyFactoryBean.serviceClass cannot be null");
        }
        return getClientProxyFactoryBean().getServiceClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    protected void prepare(Object client) {
        final Client cl = ClientProxy.getClient(client);
        if (isLogging()) {
            cl.getInInterceptors().add(new LoggingInInterceptor());
            cl.getOutInterceptors().add(new LoggingOutInterceptor());
        }

        final HTTPConduit http = (HTTPConduit) cl.getConduit();
        final HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setReceiveTimeout(getTimeOut());
        httpClientPolicy.setAllowChunking(false);
        httpClientPolicy.setConnectionTimeout(getTimeOut());
        http.setClient(httpClientPolicy);
    }

    @Override
    public Object getObject() throws Exception {
        return create();
    }

    public Object create() {
        if (getClientProxyFactoryBean() == null) {
            throw new IllegalStateException("clientProxyFactoryBean cannot be null");
        }
        final Object client = getClientProxyFactoryBean().create();
        prepare(client);

        return client;
    }
}
