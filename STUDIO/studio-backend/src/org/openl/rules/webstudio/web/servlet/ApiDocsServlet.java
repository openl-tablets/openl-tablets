package org.openl.rules.webstudio.web.servlet;

import jakarta.servlet.annotation.WebServlet;

/**
 * Answers with the page the API documentation is drawn on.
 *
 * <p>The documentation is read without logging in, so the build writes it as a page of its own, without the
 * application shell.
 *
 * @author Yury Molchan
 */
@WebServlet("/api-docs")
public class ApiDocsServlet extends FrontendPageServlet {

    public ApiDocsServlet() {
        super("/api-docs.html", "/src/api-docs.tsx");
    }
}
