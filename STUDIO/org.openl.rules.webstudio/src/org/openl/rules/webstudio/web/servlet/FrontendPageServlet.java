package org.openl.rules.webstudio.web.servlet;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.webstudio.web.Props;
import org.openl.util.StringUtils;

/**
 * Answers with a page the frontend build wrote, on which a screen is drawn in the browser.
 *
 * <p>Two things about the page depend on where the application runs: the base every relative address resolves
 * against, and the frontend dev server a developer may serve the scripts from.
 *
 * @author Yury Molchan
 */
@Slf4j
@RequiredArgsConstructor
abstract class FrontendPageServlet extends HttpServlet {

    /** What the page says its base is, written absolute by the build. */
    private static final String BUILT_BASE = "<base href=\"/\"/>";

    /** The setting naming a frontend dev server, where a developer runs one. */
    private static final String DEV_SERVER = "_REACT_UI_ROOT_";

    /** The page as the build wrote it. */
    private final String page;

    /** Its source, which a dev server serves in place of the built scripts. */
    private final String entry;

    private String template;

    @Override
    public void init() throws ServletException {
        var built = read();
        var devServer = Props.text(DEV_SERVER);
        template = StringUtils.isBlank(devServer) ? built : servedFrom(built, entry, devServer);
    }

    private String read() throws ServletException {
        try (var resource = getServletContext().getResourceAsStream(page)) {
            return new String(requireNonNull(resource, page + " resource not found").readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServletException("Failed to load the " + page + " template", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            resp.setContentType("text/html");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            // So a page kept by a browser never stands for a build that is no longer there.
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().println(basedAt(template, req.getContextPath()));
        } catch (IOException e) {
            log.error("Failed to answer the request.", e);
            if (!resp.isCommitted()) {
                resp.reset();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private static String basedAt(String template, String contextPath) {
        var base = contextPath.isEmpty() ? "/" : contextPath + "/";
        return template.replace(BUILT_BASE, "<base href=\"" + base + "\"/>");
    }

    /** The built page with its scripts swapped for the ones a frontend dev server serves. */
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
        // Quoted, because the preamble names $RefreshReg$ and $RefreshSig$, and a bare dollar in a replacement
        // stands for a capturing group.
        return built.replaceAll("(?s)<script type=\"module\".*?</head>", Matcher.quoteReplacement(scripts + "</head>"))
                .replaceAll("(?s)<link rel=\"modulepreload\".*?>", "");
    }
}
