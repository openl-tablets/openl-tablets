package org.openl.rules.ruleservice.tracing.jaeger.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;

public class JaegerConfiguration implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    private final Tracer tracer;
    private final boolean enabled;

    public JaegerConfiguration(boolean enabled, String jaegerHost, Integer jaegerPort, String applicationName) {
        this.enabled = enabled;
        if (!enabled) {
            this.tracer = NoopTracerFactory.create();
        } else {
            io.jaegertracing.Configuration.SamplerConfiguration samplerConfig = io.jaegertracing.Configuration.SamplerConfiguration
                .fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1);

            io.jaegertracing.Configuration.SenderConfiguration senderConfiguration = io.jaegertracing.Configuration.SenderConfiguration
                .fromEnv()
                .withAgentHost(jaegerHost)
                .withAgentPort(jaegerPort);

            this.tracer = io.jaegertracing.Configuration.fromEnv(applicationName)
                .withSampler(samplerConfig)
                .withReporter(io.jaegertracing.Configuration.ReporterConfiguration.fromEnv()
                    .withLogSpans(true)
                    .withSender(senderConfiguration))
                .getTracer();
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        GlobalTracer.registerIfAbsent(tracer);
    }

    @Override
    public void destroy() {
        tracer.close();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
