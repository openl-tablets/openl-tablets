package org.openl.studio.compare.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.studio.config.AsyncExecutors;

/**
 * The thread pool comparisons run on.
 *
 * <p>Comparison parses both workbooks and holds them while their tables are read, so it runs apart
 * from the pool that executes rules: a comparison of a large workbook then delays no test run, and
 * the number of workbooks parsed at once stays bounded.
 *
 * <p>The pool is sized by its core threads: a core of one would let a single comparison hold up every
 * other one until ten were already waiting.
 */
@Configuration
public class ComparisonExecutorConfiguration {

    @Bean(name = "comparisonExecutor")
    public Executor comparisonExecutor() {
        var comparisonsAtOnce = Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
        return AsyncExecutors.create("async-comparison-executor-", comparisonsAtOnce, comparisonsAtOnce);
    }
}
