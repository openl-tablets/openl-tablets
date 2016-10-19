package org.openl.rules.ruleservice.servlet;

import java.lang.reflect.Proxy;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.servlet.CXFServlet;

/**
 * OpenL Web Service CXFServlet extended Servlet.
 *
 * @author Marat Kamalov
 */
public class WSServlet extends CXFServlet {
    private static final long serialVersionUID = 1L;

    public void init() throws ServletException {
        super.init();
        ServletConfig servletConfig = getServletConfig();
        loadBus(servletConfig);
        configureDestinationRegistry(servletConfig.getInitParameter("uriEncoding"));
    }

    /**
     * Currently CXF doesn't support UTF-8 uri encoding, only ISO-8859-1.
     * This method adds decoding from any encoding functionality to DestinationRegistry.
     */
    private void configureDestinationRegistry(final String uriEncoding) {
        if (uriEncoding == null) {
            // Proceed with default implementation.
            return;
        }

        final Configurer configurer = getBus().getExtension(Configurer.class);
        getBus().setExtension(new Configurer() {
            @Override
            public void configureBean(Object beanInstance) {
                configurer.configureBean(beanInstance);
                configureHttpTransportFactory(beanInstance);
            }

            @Override
            public void configureBean(String name, Object beanInstance) {
                configurer.configureBean(name, beanInstance);
                configureHttpTransportFactory(beanInstance);
            }

            private void configureHttpTransportFactory(Object beanInstance) {
                if (beanInstance instanceof HTTPTransportFactory) {
                    HTTPTransportFactory factory = (HTTPTransportFactory) beanInstance;
                    factory.setRegistry(createDestinationRegistry(factory.getRegistry()));
                }
            }

            private DestinationRegistry createDestinationRegistry(DestinationRegistry oldRegistry) {
                if (Proxy.isProxyClass(oldRegistry.getClass()) && Proxy.getInvocationHandler(oldRegistry) instanceof DestinationRegistryInvocationHandler) {
                    return oldRegistry;
                }

                Class<? extends DestinationRegistry> type = oldRegistry.getClass();
                return (DestinationRegistry) Proxy.newProxyInstance(
                        type.getClassLoader(),
                        type.getInterfaces(),
                        new DestinationRegistryInvocationHandler(oldRegistry, uriEncoding)
                );
            }
        }, Configurer.class);
    }
}
