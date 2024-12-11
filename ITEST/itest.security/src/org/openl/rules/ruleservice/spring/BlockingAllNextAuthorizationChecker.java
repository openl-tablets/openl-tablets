package org.openl.rules.ruleservice.spring;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

@Component
@Order(3)
@Profile("custom")
public class BlockingAllNextAuthorizationChecker implements AuthorizationChecker {
    @Override
    public boolean authorize(HttpServletRequest request) {
        throw new RuntimeException("ACCESS DENIED");
    }
}
