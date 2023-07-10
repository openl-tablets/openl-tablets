package org.openl.opentelemetry.javaagent.extension;

import java.util.List;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

/**
 * Entry point of the instrumentation. Defines a shared classes which should be available on the instrumented classpath.
 *
 * @author Yury Molchan
 */
@AutoService(InstrumentationModule.class)
public class OpenLMethodsInstrumentationModule extends InstrumentationModule {

    public OpenLMethodsInstrumentationModule() {
        // Reference name for enable/disable module
        super("openl-rules");
    }

    @Override
    public List<String> getAdditionalHelperClassNames() {
        return List.of(
                MethodSingletons.class.getName(),
                OpenLMethodAttributeExtractor.class.getName());
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return List.of(new ExecutableRulesMethodSpanGenerator());
    }

}
