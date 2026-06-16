package org.openl.rules.ruleservice.spring;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import org.openl.rules.core.ce.ServiceMT;

/**
 * Shuts down the shared parallel rule-execution pool ({@link ServiceMT}) when the application context closes.
 *
 * <p>{@code ServiceMT} is a process-wide singleton backed by a {@link java.util.concurrent.ForkJoinPool} and has no
 * lifecycle of its own. Without this hook its worker threads outlive the web application, and the servlet container
 * reports them as a memory leak on undeploy.
 */
@Component
public class ServiceMTLifecycle {

    @PreDestroy
    public void destroy() {
        ServiceMT.getInstance().shutdown();
    }
}
