package org.openl.opentelemetry.javaagent.extension;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

import org.openl.rules.method.ExecutableRulesMethod;

/**
 * Composes the needed attributes to be logged in the span tags.
 *
 * @author Yury Molchan
 */
public class OpenLMethodAttributeExtractor implements AttributesExtractor<ExecutableRulesMethod, Object> {

    private static final AttributeKey<String> OPENL_TABLE_TYPE = AttributeKey.stringKey("openl.table.type");
    private static final AttributeKey<String> CODE_FUNCTION = stringKey("code.function"); // semconv v1.24.0
    private static final AttributeKey<String> CODE_NAMESPACE = stringKey("code.namespace"); // semconv v1.24.0

    @Override
    public void onStart(AttributesBuilder attributes, Context parentContext, ExecutableRulesMethod request) {
        attributes.put(CODE_FUNCTION, request.getName());
        attributes.put(CODE_NAMESPACE, request.getModuleName());
        attributes.put(OPENL_TABLE_TYPE, request.getClass().getSimpleName());
    }

    @Override
    public void onEnd(AttributesBuilder attributes, Context context, ExecutableRulesMethod request, Object response, Throwable error) {

    }
}
