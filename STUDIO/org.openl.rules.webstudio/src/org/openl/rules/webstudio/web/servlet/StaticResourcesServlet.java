package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
