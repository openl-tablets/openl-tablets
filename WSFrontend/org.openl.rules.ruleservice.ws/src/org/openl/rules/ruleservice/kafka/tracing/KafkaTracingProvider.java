package org.openl.rules.ruleservice.kafka.tracing;

import org.apache.kafka.common.header.Headers;

/**
 * SPI for tracing the Kafka service invocation
 */
public interface KafkaTracingProvider {

    String getName();

    void injectTracingHeaders(Headers consumerHeaders, Headers producerHeaders);

    Object start(Headers consumerHeaders, String name);

    void traceError(Headers consumerHeaders, String name, Exception e);

    void finish(Object span);

    String getConsumerInterceptorProviders();

    String getProducerInterceptorProviders();
}
