package org.openl.rules.ruleservice.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.openl.rules.ruleservice.RuleService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class WSServlet extends CXFServlet {
    private static final long serialVersionUID = 1L;

    public void init() throws ServletException {
        super.init();
        loadBus(getServletConfig());
        ServletContext context = getServletContext();
        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(context);

        RuleService ruleService;
        if (applicationContext.containsBean("ruleService")) {
            ruleService = (RuleService) applicationContext.getBean("ruleService");
        } else {
            throw new ServletException(
                    "Could not instaniate rule service. Make sure that you have configured bean \"ruleService\"");
        }
        ruleService.run();
    }
}
