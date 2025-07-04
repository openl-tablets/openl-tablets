package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet({"/*", "/web/help", "/web/administration/*"})
public class ReactServlet extends HttpServlet {

    private static final String HTML = """
            <!doctype html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta http-equiv="X-UA-Compatible" content="IE=edge" />
                <title>OpenL Studio</title>
                <base href="${contextPath}/" />
                <link rel="icon" href="icons/favicon.ico?v=studio" sizes="48x48" />
                <link rel="icon" href="favicon.svg?v=studio" sizes="any" type="image/svg+xml"/>
                <link rel="apple-touch-icon" href="icons/apple-touch-icon.png?v=studio"/>
                <link rel="manifest" href="icons/site.webmanifest?v=studio" />
            </head>
            <body>
                <div id="appRoot" style="min-height: 48.5px"></div>
                <script>
                    window.__APP_UI__ = "${reactUiRoot}";
                    window.__APP_API__ = "web/";
                    window.__APP_SOURCE_PATH__ = "${reactUiRoot}";
                    window.__APP_PUBLIC_PATH__ = "${contextPath}/web";
                </script>
                <script type="module" src="${reactUiRoot}/main.js"></script>
            </body>
            </html>
            """;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var reactUiRoot = System.getenv("_REACT_UI_ROOT_");
        reactUiRoot = reactUiRoot == null ? req.getContextPath() + "/javascript/ui" : reactUiRoot;
        var out = HTML
                .replace("${reactUiRoot}", reactUiRoot)
                .replace("${contextPath}", req.getContextPath());
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().println(out);
    }
}
