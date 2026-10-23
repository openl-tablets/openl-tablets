package org.openl.studio.transfer;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openl.rules.webstudio.web.servlet.SpringInitializer;

/**
 * Serves the transfer links at {@code /transfers/<id>}.
 *
 * <p>The address is no part of the REST API: a request to it carries no credential the security chains of
 * {@code /rest} could check, since the link is the credential. The security of OpenL Studio passes it without
 * authentication, and {@link TransferHandler} checks the link.
 *
 * <p>Single-user mode issues no links, so every request is answered {@code 404} there.
 *
 * @author Yury Molchan
 */
@WebServlet(urlPatterns = TransferServlet.PATH + "/*")
public class TransferServlet extends HttpServlet {

    /** The address of the transfer links in OpenL Studio. */
    public static final String PATH = "/transfers";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        var handler = SpringInitializer.getApplicationContext(getServletContext())
                .getBeanProvider(TransferHandler.class)
                .getIfAvailable();
        if (handler == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            handler.handle(request, response);
        }
    }
}
