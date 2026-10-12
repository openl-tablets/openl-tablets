package org.openl.studio.projects.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.studio.config.AsyncExecutors;

/**
 * The thread pool branch merges run on.
 *
 * <p>A merge of a large project takes minutes: it parses three versions of every conflicted workbook. It runs apart
 * from the request that started it, so no proxy cuts that request off, and apart from the pools that compile and
 * execute rules, so a merge delays neither.
 *
 * <p>Each merge holds its workbooks in memory, so only a few run at once and the rest wait in the queue.
 */
@Configuration
public class MergeExecutorConfiguration {

    private static final int MERGES_AT_ONCE = 2;

    @Bean(name = "mergeExecutor")
    public Executor mergeExecutor() {
        return AsyncExecutors.create("async-merge-executor-", MERGES_AT_ONCE, MERGES_AT_ONCE);
    }
}
