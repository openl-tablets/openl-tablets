package org.openl.rules.ruleservice.tracing.jaeger.config;

import org.openl.spring.config.ConditionalOnEnable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@Component
@ConditionalOnEnable("ruleservice.tracing.jaeger.enabled")
public class JaegerConfiguration implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    private final Tracer tracer;

    public JaegerConfiguration(@Value("${jaeger.service.name:RuleServices}") String applicationName,
            @Value("${jaeger.agent.host:localhost}") String jaegerHost,
            @Value("${jaeger.agent.port:6831}") Integer jaegerPort) {
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

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        GlobalTracer.registerIfAbsent(tracer);
    }

    @Override
    public void destroy() {
        tracer.close();
    }

}
