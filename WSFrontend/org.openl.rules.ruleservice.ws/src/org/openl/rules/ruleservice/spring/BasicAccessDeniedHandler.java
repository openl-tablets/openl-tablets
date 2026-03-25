package org.openl.rules.ruleservice.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AccessDeniedHandler;

/**
 * Add the 'WWW-Authenticate: Basic' header if the 'Authorization' header is missing. This will prompt the browser
 * to display a login popup, allowing the user to enter their login credentials.
 *
 * @author Yury Molchan
 */
@Order
@Component
@Slf4j
public class BasicAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        log.info("Access denied: {} {};", request.getMethod(), request.getRequestURL());
        var credentials = request.getHeader("Authorization");
        if (credentials == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("WWW-Authenticate", "Basic");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
