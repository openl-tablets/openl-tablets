package org.openl.rules.webstudio.projects.tests.executor.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
public class TestSuiteExecutorsConfiguration {

    @Bean(name = "testSuiteExecutor")
    public Executor testSuiteExecutor() {
        var availableProcessors =  Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setThreadNamePrefix("async-test-suite-executor-");
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(Math.max(availableProcessors, 2));
        exec.setQueueCapacity(10);
        exec.setAwaitTerminationSeconds(5);
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(exec);
    }

}
