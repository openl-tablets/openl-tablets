package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.openl.spring.env.DefaultPropertySource;

/**
 * Download the `application.properties` file with description of all properties.
 *
 * @author Yury Mmolchan
 */
@Slf4j
@WebServlet("/application.properties")
public class AppPropertiesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        try {
            DefaultPropertySource.transferAllOpenLDefaultProperties(resp.getOutputStream());
        } catch (IOException e) {
            log.error("Failed to write the OpenL default properties.", e);
            failed(resp);
        }
    }

    /** Answers with an error, unless the response has already gone out. */
    private static void failed(HttpServletResponse resp) {
        if (!resp.isCommitted()) {
            resp.reset();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
