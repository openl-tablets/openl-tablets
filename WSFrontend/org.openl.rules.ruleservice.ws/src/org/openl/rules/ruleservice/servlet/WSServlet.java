package org.openl.rules.ruleservice.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    }
    
    @Override
    protected void redirect(HttpServletRequest request,
            HttpServletResponse response,
            String pathInfo) throws ServletException {
        if (pathInfo == null){ //Fix issue with empty path on linux
            pathInfo = "/";
        }
        super.redirect(request, response, pathInfo);
    }

}
