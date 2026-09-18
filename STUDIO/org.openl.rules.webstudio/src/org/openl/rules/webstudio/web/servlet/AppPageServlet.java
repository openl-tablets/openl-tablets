package org.openl.rules.webstudio.web.servlet;

import jakarta.servlet.annotation.WebServlet;

/**
 * Answers every address the application is opened at with the page it is drawn on.
 *
 * <p>The widest mapping there is, and the last one matched: static files, the API documentation, the REST API and
 * the health checks each carry a narrower one.
 *
 * @author Yury Molchan
 */
@WebServlet("/*")
public class AppPageServlet extends FrontendPageServlet {

    public AppPageServlet() {
        super("/index.html", "/src/index.tsx");
    }
}
