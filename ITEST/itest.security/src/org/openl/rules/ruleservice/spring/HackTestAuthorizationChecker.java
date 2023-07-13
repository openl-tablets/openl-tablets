package org.openl.rules.ruleservice.spring;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

@Component
@Order(2)
@Profile("custom")
public class HackTestAuthorizationChecker implements AuthorizationChecker {

    @Override
    public boolean authorize(HttpServletRequest request) {
        String hackHeader = request.getHeader("hack-header");
        return hackHeader != null && !hackHeader.isEmpty();
    }

}
