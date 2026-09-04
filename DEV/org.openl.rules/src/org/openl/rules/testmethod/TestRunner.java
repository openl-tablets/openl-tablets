package org.openl.rules.testmethod;

import lombok.RequiredArgsConstructor;

import org.openl.vm.IRuntimeEnv;

@RequiredArgsConstructor
public class TestRunner {

    private final ITestResultBuilder resultBuilder;

    @SuppressWarnings("unchecked")
    public ITestUnit runTest(TestDescription test,
                             Object target,
                             IRuntimeEnv env,
                             int ntimes) {
        if (ntimes <= 0) {
            return runTest(test, target, env, 1);
        } else {
            Object res = null;
            Throwable exception = null;
            var oldContext = env.getContext();
            long time;
            var start = System.nanoTime(); // Initialization here is needed if exception is thrown
            long end;
            try {
                var context = test.getRuntimeContext();
                env.setContext(context);
                var args = test.getArguments();
                var testedMethod = test.getTestedMethod();
                // Measure only actual test run time
                start = System.nanoTime();
                for (var j = 0; j < ntimes; j++) {
                    res = testedMethod.invoke(target, args, env);
                }
                end = System.nanoTime();
            } catch (Exception | LinkageError | StackOverflowError t) {
                end = System.nanoTime();
                exception = t;
            } finally {
                env.setContext(oldContext);
            }
            time = end - start;
            return resultBuilder.build(test, res, exception, time);
        }
    }

}
