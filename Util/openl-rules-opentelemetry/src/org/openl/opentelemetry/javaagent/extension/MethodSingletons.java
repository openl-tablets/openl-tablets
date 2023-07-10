package org.openl.opentelemetry.javaagent.extension;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;

import org.openl.rules.method.ExecutableRulesMethod;

/**
 * The reason why this singleton is separated from the {@link ExecutableRulesMethodSpanGenerator} is to be simple for using
 * in the bytebuddy aspects and do not solve the issues with the java agent and execution classloaders.
 *
 * @author Yury Molchan
 */
public final class MethodSingletons {

    // The name of the instrumentation scope (otel.scope.name attribute in span attributes)
    private static final String INSTRUMENTATION_NAME = "openl-rules-opentelemetry";

    private static final Instrumenter<ExecutableRulesMethod, Void> INSTRUMENTER;

    static {
        INSTRUMENTER = Instrumenter.<ExecutableRulesMethod, Void>builder(GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME, ExecutableRulesMethod::getName)
                .addAttributesExtractor(new OpenLMethodAttributeExtractor())
                .buildInstrumenter(SpanKindExtractor.alwaysInternal());
    }

    public static Instrumenter<ExecutableRulesMethod, Void> instrumenter() {
        return INSTRUMENTER;
    }

    private MethodSingletons() {
    }
}
