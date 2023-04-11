package org.openl.rules.ruleservice.spring;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@Order(4)
@Profile("custom")
public class AlwaysValidateAuthorizationChecker implements AuthorizationChecker {
    @Override
    public boolean authorize(HttpServletRequest request) {
        return true;
    }
}
