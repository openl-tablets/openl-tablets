package org.openl.studio.mcp;

import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openl.rules.webstudio.web.servlet.SpringInitializer;

/**
 * Serves the MCP server built into OpenL Studio at {@code /mcp}.
 *
 * <p>A request passes the filters of OpenL Studio before it gets here, so the security of OpenL Studio checks the
 * credentials of an MCP client the way it checks the ones of a REST call. {@link McpProxy} then sends the request on
 * to the server and streams the answer back as the server writes it, however long the server takes.
 *
 * <p>A request is answered with {@code 503} while the server does not run.
 *
 * @author Yury Molchan
 */
@WebServlet(urlPatterns = McpProxyServlet.PATH + "/*", asyncSupported = true)
public class McpProxyServlet extends HttpServlet {

    /** The address of the MCP server in OpenL Studio. */
    public static final String PATH = "/mcp";

    private final transient McpProxy proxy = new McpProxy();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var server = SpringInitializer.getApplicationContext(getServletContext())
                .getBean(McpServerProcess.class)
                .address();
        if (server == null) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            proxy.proxy(request, response, server);
        }
    }

    @Override
    public void destroy() {
        proxy.close();
    }
}
