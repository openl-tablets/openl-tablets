package org.openl.studio.projects.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.studio.config.AsyncExecutors;

@Configuration
public class TestSuiteExecutorsConfiguration {

    @Bean(name = "testSuiteExecutor")
    public Executor testSuiteExecutor() {
        var availableProcessors = Runtime.getRuntime().availableProcessors();
        return AsyncExecutors.create("async-test-suite-executor-", 2, Math.max(availableProcessors, 2));
    }

}
