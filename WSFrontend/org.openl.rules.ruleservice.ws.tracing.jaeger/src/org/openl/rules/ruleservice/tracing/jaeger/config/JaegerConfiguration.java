package org.openl.rules.ruleservice.tracing.jaeger.config;

import org.openl.spring.config.ConditionalOnEnable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@Component
@ConditionalOnEnable("ruleservice.tracing.jaeger.enabled")
public class JaegerConfiguration implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    private final Tracer tracer;

    public JaegerConfiguration(@Value("${jaeger.service.name}") String applicationName,
            @Value("${jaeger.agent.host}") String jaegerHost,
            @Value("${jaeger.agent.port}") Integer jaegerPort) {
        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
            .withType(ConstSampler.TYPE)
            .withParam(1);

        Configuration.SenderConfiguration senderConfiguration = Configuration.SenderConfiguration.fromEnv()
            .withAgentHost(jaegerHost)
            .withAgentPort(jaegerPort);

        this.tracer = Configuration.fromEnv(applicationName)
            .withSampler(samplerConfig)
            .withReporter(
                Configuration.ReporterConfiguration.fromEnv().withLogSpans(true).withSender(senderConfiguration))
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
