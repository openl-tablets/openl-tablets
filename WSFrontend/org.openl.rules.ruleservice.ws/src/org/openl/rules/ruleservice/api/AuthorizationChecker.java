package org.openl.rules.ruleservice.api;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface for defining authorization checker that can be added to the Spring context  to filter incoming HTTP requests.
 * <p>
 * Multiple instances of the checkers can be added to the Spring context, and incoming requests are filtered through
 * a list of validators. The order of validators is determined by their initialization order in the Spring context.
 * <p>
 * If no checkers are present in the Spring context, then any request will be permitted.
 * If some checker returns a positive result for a request, the request is permitted.
 * If all checkers return a negative result for a request, the request is denied.
 * If some checker throws an exception for a request, the request is denied and the rest checkers are ignored.
 *
 * @author Yury Molchan
 */
@FunctionalInterface
public interface AuthorizationChecker {

    /**
     * Checks if the given request is allowed to be processed.
     * Usually, implementation checks Authorization HTTP header and make decision, if a requested URL can be processed.
     *
     * @param request Http request.
     * @return true if the request is allowed to be processed, false otherwise.
     */
    boolean authorize(HttpServletRequest request);

}

