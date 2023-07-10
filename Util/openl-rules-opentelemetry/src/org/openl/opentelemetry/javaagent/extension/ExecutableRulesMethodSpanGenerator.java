package org.openl.opentelemetry.javaagent.extension;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.openl.opentelemetry.javaagent.extension.MethodSingletons.instrumenter;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.openl.rules.method.ExecutableRulesMethod;

/**
 * Wraps the needed classes of the OpenL methods for registering spans.
 *
 * @author Yury Molchan
 */
public class ExecutableRulesMethodSpanGenerator implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("org.openl.rules.method.ExecutableRulesMethod");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(named("invoke"), MethodAdvice.class.getName());
    }

    @SuppressWarnings("unused")
    public static class MethodAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(
                @Advice.This ExecutableRulesMethod openLTable,
                @Advice.Local("otelContext") Context context,
                @Advice.Local("otelScope") Scope scope) {

            Context parentContext = Context.current();

            if (!instrumenter().shouldStart(parentContext, openLTable)) {
                return;
            }

            context = instrumenter().start(parentContext, openLTable);
            scope = context.makeCurrent();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void stopSpan(
                @Advice.This ExecutableRulesMethod openLTable,
                @Advice.Local("otelContext") Context context,
                @Advice.Local("otelScope") Scope scope,
                @Advice.Thrown Throwable throwable) {
            scope.close();

            instrumenter().end(context, openLTable, null, throwable);
        }
    }

}
