package org.openl.rules.ruleservice.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * OpenL Web Service CXFServlet extended Servlet.
 * 
 * @author Marat Kamalov
 * 
 */
public class WSServlet extends CXFServlet {
    private static final long serialVersionUID = 1L;

    public void init() throws ServletException {
        super.init();
        loadBus(getServletConfig());
        ServletContext context = getServletContext();
        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(context);

        ServiceManagerImpl serviceManager;
        if (applicationContext.containsBean("serviceManager")) {
            serviceManager = (ServiceManagerImpl) applicationContext.getBean("serviceManager");
        } else {
            throw new ServletException(
                    "Could not instaniate serice manager. Make sure that you have configured bean \"ruleService\"");
        }
        serviceManager.start();
    }
}
