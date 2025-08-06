package org.openl.rules.ruleservice.spring;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AccessDeniedHandler;

@Component
@Profile("custom")
@Order(1000000)
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getHeader("Authorization") == null) {
            response.setStatus(777);
            response.addHeader("hack-header", "wow!");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
