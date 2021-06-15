package org.openl.rules.ruleservice.tracing.jaeger.kafka;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.header.Headers;
import org.openl.rules.ruleservice.kafka.tracing.KafkaTracingProvider;
import org.openl.spring.config.ConditionalOnEnable;
import org.springframework.stereotype.Component;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingConsumerInterceptor;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.opentracing.contrib.kafka.TracingProducerInterceptor;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

@Component
@ConditionalOnEnable("ruleservice.tracing.jaeger.enabled")
public class KafkaTracingProviderImpl implements KafkaTracingProvider {

    private static final String NAME = "Jaeger";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * This method injects the tracing headers from consumer to the producer for span context propagation.
     * 
     * @param consumerHeaders
     * @param producerHeaders
     */
    @Override
    public void injectTracingHeaders(Headers consumerHeaders, Headers producerHeaders) {
        if (GlobalTracer.isRegistered()) {
            Tracer tracer = GlobalTracer.get();
            SpanContext spanContext = TracingKafkaUtils.extractSpanContext(consumerHeaders, tracer);
            TracingKafkaUtils.inject(spanContext, producerHeaders, tracer);
        }

    }

    /**
     * Start span as a child of context which comes with consumer headers.
     * 
     * @return active span
     */
    @Override
    public Object start(Headers consumerHeaders, String name) {
        if (GlobalTracer.isRegistered()) {
            Tracer tracer = GlobalTracer.get();
            SpanContext spanContext = TracingKafkaUtils.extractSpanContext(consumerHeaders, tracer);
            return tracer.buildSpan(name).withTag("Service Name", name).asChildOf(spanContext).start();
        } else {
            return null;
        }
    }

    @Override
    public void traceError(Headers consumerHeaders, String name, Exception e) {
        if (GlobalTracer.isRegistered()) {
            Tracer tracer = GlobalTracer.get();
            SpanContext spanContext = TracingKafkaUtils.extractSpanContext(consumerHeaders, tracer);
            Span errorSpan = tracer.buildSpan(name).withTag("Service Name", name).asChildOf(spanContext).start();
            Tags.ERROR.set(errorSpan, true);
            Map<String, StringWriter> fields = new HashMap<>();
            StringWriter errors = new StringWriter();
            try (PrintWriter s = new PrintWriter(errors)) {
                e.printStackTrace(s);
                fields.put("stack", errors);
                errorSpan.log(fields);
            }
            errorSpan.finish();
        }
    }

    /**
     * Finish the current active span
     *
     */
    @Override
    public void finish(Object span) {
        if (span != null) {
            ((Span) span).finish();
        }
    }

    @Override
    public String getConsumerInterceptorProviders() {
        return TracingConsumerInterceptor.class.getName();
    }

    @Override
    public String getProducerInterceptorProviders() {
        return TracingProducerInterceptor.class.getName();
    }
}
