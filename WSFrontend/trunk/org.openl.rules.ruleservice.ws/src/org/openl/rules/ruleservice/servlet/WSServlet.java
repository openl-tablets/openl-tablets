package org.openl.rules.ruleservice.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.RuleServiceBase;
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

        RuleServiceBase ruleService;
        if (applicationContext.containsBean("ruleService")) {
            ruleService = (RuleServiceBase) applicationContext.getBean("ruleService");
        } else {
            try {
                ruleService = new RuleServiceMain();
            } catch (RRepositoryException e) {
                throw new ServletException(e);
            }
        }
        WebServicesDeployAdmin deployAdmin = (WebServicesDeployAdmin)ruleService.getPublisher().getDeployAdmin();
        deployAdmin.setContext(applicationContext);
        ruleService.run();
    }
}
