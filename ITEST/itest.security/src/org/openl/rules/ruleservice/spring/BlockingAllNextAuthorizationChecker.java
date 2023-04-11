package org.openl.rules.ruleservice.spring;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@Order(3)
@Profile("custom")
public class BlockingAllNextAuthorizationChecker implements AuthorizationChecker {
    @Override
    public boolean authorize(HttpServletRequest request) {
        throw new RuntimeException("ACCESS DENIED");
    }
}
