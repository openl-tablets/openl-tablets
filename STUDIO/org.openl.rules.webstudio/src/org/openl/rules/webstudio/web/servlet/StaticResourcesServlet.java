package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Explicit handling of the static resources to be nonconflict with the React application.
 *
 * @author Yury Mmolchan
 */
@WebServlet({"/css/*", "/icons/*", "/images/*", "/js/*", "/javascript/*", "/favicon.ico", "/favicon.svg"})
public class StaticResourcesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(req, resp);;
    }
}
