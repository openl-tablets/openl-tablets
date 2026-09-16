package org.openl.rules.webstudio.web.servlet;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openl.rules.webstudio.web.Props;
import org.openl.util.StringUtils;

/**
 * Answers every address the application is opened at with the page it is drawn on.
 *
 * <p>The page is what the frontend build wrote: it already names the scripts and stylesheets it needs, under the
 * names that build gave them. Only two things about it depend on where the application is running — the base every
 * relative address is read against, and, while a developer has the frontend running beside the server, the address
 * its scripts are served from.
 *
 * <p>The API documentation is a page of its own, built beside this one and read without logging in, so its
 * address is answered with that page instead.
 *
 * <p>A file the build left beside the page is handed to the container to serve, so that it keeps its content type
 * and its caching headers.
 *
 * @author Yury Mmolchan
 */
@WebServlet("/*")
public class StaticResourcesServlet extends HttpServlet {

    /** What the page says its base is, written absolute by the build. */
    private static final String BUILT_BASE = "<base href=\"/\"/>";

    /** The setting naming a frontend served from elsewhere, which a developer runs beside the server. */
    private static final String DEV_SERVER = "_REACT_UI_ROOT_";

    /** The page the application is drawn on, and the source the frontend is written in. */
    private static final String APP_PAGE = "/index.html";
    private static final String APP_ENTRY = "/src/index.tsx";

    /** Where the API documentation is read, the page the build wrote for it, and its own source. */
    private static final String API_DOCS = "/api-docs";
    private static final String API_DOCS_PAGE = "/api-docs.html";
    private static final String API_DOCS_ENTRY = "/src/api-docs.tsx";

    private String htmlTemplate;
    private String apiDocsTemplate;

    @Override
    public void init() throws ServletException {
        var devServer = Props.text(DEV_SERVER);
        htmlTemplate = page(APP_PAGE, APP_ENTRY, devServer);
        apiDocsTemplate = page(API_DOCS_PAGE, API_DOCS_ENTRY, devServer);
    }

    /** A page the build wrote, or the same page served by a frontend dev server where a developer runs one. */
    private String page(String page, String entry, String devServer) throws ServletException {
        var built = read(page);
        return StringUtils.isBlank(devServer) ? built : servedFrom(built, entry, devServer);
    }

    /** One of the pages the frontend build wrote. */
    private String read(String page) throws ServletException {
        try (var resource = getServletContext().getResourceAsStream(page)) {
            return new String(requireNonNull(resource, page + " resource not found").readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServletException("Failed to load the " + page + " template", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // The context root requested without the trailing slash carries no path, and names the same page as "/".
        var path = requireNonNullElse(req.getPathInfo(), "/");
        // Check if the request is for a static resource
        if (path.startsWith("/assets/")
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
        var contextPath = req.getContextPath();
        var page = API_DOCS.equals(path) ? apiDocsTemplate : htmlTemplate;
        var out = page.replace(BUILT_BASE,
                "<base href=\"" + (contextPath.isEmpty() ? "/" : contextPath + "/") + "\"/>");

        resp.setContentType("text/html");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Drawn afresh by the application every time it is opened, so a page kept by a browser never stands for a
        // build that is no longer there.
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().println(out);
    }

    /**
     * The built page with its scripts swapped for the ones a frontend dev server serves: the build's scripts are
     * the ones it wrote, and there are none of those to serve yet.
     */
    static String servedFrom(String built, String entry, String devServer) {
        var root = devServer.endsWith("/") ? devServer.substring(0, devServer.length() - 1) : devServer;
        var scripts = """
                <script type="module">
                  import RefreshRuntime from '%s/@react-refresh'
                  RefreshRuntime.injectIntoGlobalHook(window)
                  window.$RefreshReg$ = () => {}
                  window.$RefreshSig$ = () => (type) => type
                  window.__vite_plugin_react_preamble_installed__ = true
                </script>
                <script type="module" src="%s/@vite/client"></script>
                <script type="module" src="%s%s"></script>
                """.formatted(root, root, root, entry);
        // Everything the build wrote into the head is its own; the dev server serves its own instead. The
        // replacement is quoted because the React Refresh preamble names $RefreshReg$ and $RefreshSig$, and a
        // bare dollar in a replacement stands for a capturing group.
        return built.replaceAll("(?s)<script type=\"module\".*?</head>", Matcher.quoteReplacement(scripts + "</head>"))
                .replaceAll("(?s)<link rel=\"modulepreload\".*?>", "");
    }
}
