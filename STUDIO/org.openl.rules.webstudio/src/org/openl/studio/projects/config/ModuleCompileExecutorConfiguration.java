package org.openl.studio.projects.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * The pool that opens modules away from the requests that asked for them.
 *
 * <p>A module compile holds its thread for as long as the compilation takes, which on a large project is minutes.
 * It is kept apart from the test-suite pool so a compile never delays a test run, and the other way round.
 */
@Configuration
public class ModuleCompileExecutorConfiguration {

    @Bean(name = "moduleCompileExecutor")
    public Executor moduleCompileExecutor() {
        var availableProcessors = Runtime.getRuntime().availableProcessors();
        var exec = new ThreadPoolTaskExecutor();
        exec.setThreadNamePrefix("async-module-compile-executor-");
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(Math.max(availableProcessors, 2));
        exec.setQueueCapacity(10);
        exec.setAwaitTerminationSeconds(5);
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(exec);
    }

}
