package org.openl.rules.ruleservice.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.RuleServiceMain;
import org.openl.rules.ruleservice.publish.WebServicesDeployAdmin;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class WSServlet extends CXFServlet {
    private static final long serialVersionUID = 1L;

    public void init() throws ServletException {
        super.init();
        loadBus(getServletConfig());
        ServletContext context = getServletContext();
        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(context);

        WebServicesDeployAdmin admin;
        if (applicationContext.containsBean("deploymentAdmin")) {
            admin = (WebServicesDeployAdmin) applicationContext.getBean("deploymentAdmin");
        } else {
            admin = new WebServicesDeployAdmin();
        }
        admin.setDestinationFactory(servletTransportFactory);
        RuleServiceMain ruleService = new RuleServiceMain();
        ruleService.setDeployAdmin(admin);
        try {
            ruleService.runFrontend();
        } catch (RRepositoryException e) {
            e.printStackTrace();
        }
    }
}
