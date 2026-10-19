package org.openl.studio.common;

import org.jspecify.annotations.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Binds a request to the running test, as a request being served is bound to the thread serving it.
 *
 * @author Yury Molchan
 */
public final class CurrentRequest {

    private CurrentRequest() {
    }

    /**
     * Binds a {@code GET} request of the address.
     *
     * @param uri   the path of the request
     * @param query the query of the request, or {@code null} for none
     * @return the response being built for the request
     */
    public static MockHttpServletResponse bind(String uri, @Nullable String query) {
        var request = new MockHttpServletRequest("GET", uri);
        request.setQueryString(query);
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        return response;
    }

    /** Unbinds the request, as the end of serving it does. */
    public static void reset() {
        RequestContextHolder.resetRequestAttributes();
    }
}
