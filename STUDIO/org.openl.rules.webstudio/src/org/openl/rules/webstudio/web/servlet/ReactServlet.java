package org.openl.rules.webstudio.web.servlet;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class ReactServlet extends HttpServlet {

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
