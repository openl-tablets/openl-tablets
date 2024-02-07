package org.openl.rules.ruleservice.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.springframework.context.ApplicationContext;

import org.openl.rules.context.RulesRuntimeContextFactory;

@WebServlet(urlPatterns = "/*", loadOnStartup = 1, initParams = {
        @WebInitParam(name = "service-list-path", value = "cxf-services"),
        @WebInitParam(name = "hide-service-list-page", value = "true")})
public class CXFServlet extends CXFNonSpringServlet {

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        ApplicationContext ac = SpringInitializer.getApplicationContext(servletConfig.getServletContext());
        Bus cxf = ac.getBean("cxf", Bus.class);
        setBus(cxf);
    }

    @Override
    protected void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        RulesRuntimeContextFactory.setLocale(request.getLocale());
        try {
            super.invoke(request, response);
        } finally {
            RulesRuntimeContextFactory.removeLocale();
        }
    }
}
