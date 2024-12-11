package org.openl.rules.ruleservice.spring;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

@Component
@Order(4)
@Profile("custom")
public class AlwaysValidateAuthorizationChecker implements AuthorizationChecker {
    @Override
    public boolean authorize(HttpServletRequest request) {
        return true;
    }
}
