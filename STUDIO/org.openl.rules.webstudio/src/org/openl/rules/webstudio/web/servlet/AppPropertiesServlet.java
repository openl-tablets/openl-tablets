package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
