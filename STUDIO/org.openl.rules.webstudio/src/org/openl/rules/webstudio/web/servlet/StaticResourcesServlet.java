package org.openl.rules.webstudio.web.servlet;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
@WebServlet("/*")
public class StaticResourcesServlet extends HttpServlet {

    private String htmlTemplate;

    @Override
    public void init() throws ServletException {
        try (var resource = getServletContext().getResourceAsStream("/index.html")) {
            htmlTemplate = new String(requireNonNull(resource, "index.html resource not found").readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServletException("Failed to load index.html template", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        var path = req.getPathInfo();
        // Check if the request is for a static resource
        if (path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/javascript/")
                || path.startsWith("/images/")
                || path.startsWith("/icons/")
                || path.equals("/favicon.svg")
                || path.equals("/favicon.ico")) {

            // Forward the request to the container's "default" servlet.
            // This is the standard, portable way to handle static resources
            // This servlet correctly handles content types, caching headers (ETag, Last-Modified)
            getServletContext().getNamedDispatcher("default").forward(req, resp);
            return;
        }

        // Handling index.html for the React application
        var out = htmlTemplate.replace("base href=\"/", "base href=\"" + req.getContextPath() + "/");

        var reactUiRoot = System.getenv("_REACT_UI_ROOT_");
        if (reactUiRoot != null) {
            out = out.replace("script src=\"js/", "script src=\"" + reactUiRoot + "/");
        }

        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().println(out);
    }
}
