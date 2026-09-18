package org.openl.rules.webstudio.web.servlet;

import static java.util.Objects.requireNonNullElse;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.MappingMatch;

import lombok.extern.slf4j.Slf4j;

/**
 * Hands a file the frontend build left beside the pages to the container to serve.
 *
 * <p>Each address is named, because {@link AppPageServlet} answers everything left over with a page.
 *
 * @author Yury Molchan
 */
@Slf4j
@WebServlet({"/assets/*", "/icons/*", "/favicon.svg", "/favicon.ico"})
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
        try {
            // The container's own servlet is the portable way: it sets the content type and the caching headers.
            getServletContext().getNamedDispatcher(CONTAINER_SERVLET).forward(asServedByTheContainer(req), resp);
        } catch (ServletException | IOException e) {
            log.error("Failed to answer the request.", e);
            if (!resp.isCommitted()) {
                resp.reset();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
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
