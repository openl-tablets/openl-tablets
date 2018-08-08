package org.openl.rules.testmethod;

import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class TestRunner {

    private final ITestResultBuilder resultBuilder;

    public TestRunner(ITestResultBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
    }

    @SuppressWarnings("unchecked")
    public ITestUnit runTest(TestDescription test, Object target, IRuntimeEnv env, long ntimes) {
        if (ntimes <= 0) {
            return runTest(test, target, env, 1);
        } else {
            Object res = null;
            Throwable exception = null;
            IRuntimeContext oldContext = env.getContext();
            long time;
            long start = System.nanoTime(); // Initialization here is needed if exception is thrown
            long end;
            try {
                IRuntimeContext context = test.getRuntimeContext();
                env.setContext(context);
                Object[] args = test.getArguments();
                IOpenMethod testedMethod = test.getTestedMethod();
                // Measure only actual test run time
                start = System.nanoTime();
                for (int j = 0; j < ntimes; j++) {
                    res = testedMethod.invoke(target, args, env);
                }
                end = System.nanoTime();
            } catch (Throwable t) {
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
