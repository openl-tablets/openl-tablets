package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the root application path instead of using <welcome-file> in web.xml
 *
 * @author Yury Mmolchan
 */
@WebServlet("")
public class DefaultServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        getServletContext().getRequestDispatcher("/faces/pages/modules/index.xhtml").forward(req, resp);
    }
}
