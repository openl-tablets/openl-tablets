package org.openl.rules.webstudio.web.servlet;

import static java.util.Objects.requireNonNullElse;

import java.io.IOException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.MappingMatch;

/**
 * Hands a file the build left beside the pages to the container to serve.
 *
 * <p>The files are the assets of the frontend build, and the lists of the third-party libraries under
 * {@code /licenses}: one the frontend build writes, one the war build writes.
 *
 * <p>Each address is named, because {@link AppPageServlet} answers everything left over with a page.
 *
 * @author Yury Molchan
 */
@WebServlet({"/assets/*", "/icons/*", "/licenses/*", "/favicon.svg", "/favicon.ico"})
public class StaticResourcesServlet extends HttpServlet {

    /** The name every container knows its own file-serving servlet by. */
    private static final String CONTAINER_SERVLET = "default";

    /** What a container reports for a request that servlet answers: the whole address, matched by the root. */
    private static final HttpServletMapping CONTAINER_MAPPING = new HttpServletMapping() {

        @Override
        public String getMatchValue() {
            return "";
        }

        @Override
        public String getPattern() {
            return "/";
        }

        @Override
        public String getServletName() {
            return CONTAINER_SERVLET;
        }

        @Override
        public MappingMatch getMappingMatch() {
            return MappingMatch.DEFAULT;
        }
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        serve(getServletContext(), req, resp);
    }

    /**
     * Answers with the file at the address of the request, or with an error when the container cannot serve it.
     *
     * <p>Any servlet mapped at a prefix hands a file over this way, so the container answers it whole.
     */
    static void serve(ServletContext context, HttpServletRequest req, HttpServletResponse resp) {
        try {
            // The container's own servlet is the portable way: it sets the content type and the caching headers.
            context.getNamedDispatcher(CONTAINER_SERVLET).forward(asServedByTheContainer(req), resp);
        } catch (ServletException | IOException e) {
            FrontendPageServlet.failed(resp, e);
        }
    }

    /**
     * The request as the container's own servlet expects one: the whole address in the servlet path, matched the
     * way the container matches what it serves itself.
     *
     * <p>A mapping narrower than everything splits the address in two, and which half a container reads back
     * depends on how the address was matched. Jetty reads the path info of a prefix match and the servlet path of
     * an exact one, so either half alone loses files: {@code /assets/x.js} is looked for under the root as
     * {@code /x.js}, or {@code /favicon.svg} as nothing at all. One whole address, matched as the container's
     * own, is the shape every container reads.
     */
    private static HttpServletRequest asServedByTheContainer(HttpServletRequest req) {
        var address = req.getServletPath() + requireNonNullElse(req.getPathInfo(), "");
        return new HttpServletRequestWrapper(req) {
            @Override
            public String getServletPath() {
                return address;
            }

            @Override
            public String getPathInfo() {
                return null;
            }

            @Override
            public HttpServletMapping getHttpServletMapping() {
                return CONTAINER_MAPPING;
            }
        };
    }
}
