package org.openl.rules.webstudio.web.servlet;

import javax.servlet.ServletConfig;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class WebStudioCXFServlet extends CXFServlet {
    private final Logger log = LoggerFactory.getLogger(WebStudioCXFServlet.class);
    private boolean busCreated;

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        try {
            super.loadBus(servletConfig);
        } catch (NoSuchBeanDefinitionException e) {
            // In Install Wizard CXF is not configured and it's ok that spring bean for CXF is not found.
            // Finish servlet initialization correctly: initialize the bus ourselves (and lately destroy it ourselves).
            log.info("Bean '" + e.getBeanName() + "' is not found. Create bus ourselves.");

            setBus(BusFactory.newInstance().createBus());
            busCreated = true;
        } catch (RuntimeException e) {
            // Log unexpected exception and rethrow it.
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void destroyBus() {
        super.destroyBus();

        if (busCreated) {
            // If we created the Bus, we must destroy it.
            getBus().shutdown(true);
            setBus(null);
        }
    }
}