package org.openl.rules.ruleservice.api;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Process access denied when no one of {@linkplain  AuthorizationChecker} authorize the connection.
 * This interface can have multiple implementations, but the first fill be selected according to the
 * {@linkplain org.springframework.core.annotation.Order} annotation.
 *
 * @author Yury Molchan
 */
@FunctionalInterface
public interface AccessDeniedHandler {

    /**
     * Handles an access denied failure.
     *
     * @param request  that resulted in an <code>AccessDeniedException</code>
     * @param response so that the user agent can be advised of the failure
     * @throws IOException      in the event of an IOException
     * @throws ServletException in the event of a ServletException
     */
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
}
