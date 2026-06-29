package org.openl.rules.ruleservice.spring;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import org.openl.rules.core.ce.ServiceMT;

/**
 * Shuts down the shared parallel rule-execution service ({@link ServiceMT}) when the application context closes.
 *
 * <p>{@code ServiceMT} is a process-wide singleton that runs parallel rule evaluations on virtual threads and has no
 * lifecycle of its own. This hook stops accepting new tasks and lets in-flight evaluations finish (cancelling any that
 * overrun a short grace period), so none keep running rule logic after the web application is undeployed.
 */
@Component
public class ServiceMTLifecycle {

    @PreDestroy
    public void destroy() {
        ServiceMT.getInstance().shutdown();
    }
}
