package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.spring.env.DefaultPropertySource;

/**
 * Download the `application.properties` file with description of all properties.
 *
 * @author Yury Mmolchan
 */
@WebServlet("/application.properties")
public class AppPropertiesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        DefaultPropertySource.transferAllOpenLDefaultProperties(resp.getOutputStream());
    }
}
