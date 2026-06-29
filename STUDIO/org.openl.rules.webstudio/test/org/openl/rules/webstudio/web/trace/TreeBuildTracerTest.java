package org.openl.rules.webstudio.web.trace;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.webstudio.web.trace.debug.DebugHook;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

class TreeBuildTracerTest {

    /** A pass-through hook: enough to put the current thread into debug mode for this test. */
    private static final DebugHook NOOP_HOOK = new DebugHook() {
        @Override
        public <T, E extends IRuntimeEnv, R> R bracketInvoke(Invokable<? super T, E> executor, T target,
                                                             Object[] params, E env, Object source) {
            return executor.invoke(target, params, env);
        }

        @Override
        public void onPut(Object source, String id, Object[] args) {
            // no-op
        }
    };

    @Test
    void aDebugSessionReportsTracingAsOnSoParallelismIsDisabled() {
        // ServiceMT runs a MultiCall fan-out inline while Tracer.isEnabled(), so the fanned-out calls stay
        // on the worker thread and are captured by its debug hook — the trace has no holes. That hinges on
        // a debug session reading as "tracing on".
        TreeBuildTracer.enableDebug(NOOP_HOOK);
        try {
            assertTrue(Tracer.isEnabled(), "a debug session must read as tracing-on so ServiceMT runs sequentially");
        } finally {
            TreeBuildTracer.disableDebug();
        }
    }
}
