package org.openl.rules.method;

import java.io.IOException;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.CacheMode;
import org.openl.rules.vm.SimpleRulesRuntimeEnvUtils;

public class ExecutableRulesMethodTest {

    private static String __src = "test/rules/MethodArgumentsCacheTest.xls";

    private RulesEngineFactory<A1> engineFactory;
    private A1 instance;

    public static interface A1 {
        String Random(Boolean a1);

        String Random2(Boolean a1);
    }

    @Before
    public void init() throws IOException {
        engineFactory = new RulesEngineFactory<A1>(__src, A1.class);
        instance = engineFactory.newEngineInstance();
    }

    @Test
    public void cacheTest() {
        SimpleRulesRuntimeEnvUtils.setMethodArgumentsCacheEnable(instance, true);
        SimpleRulesRuntimeEnvUtils.setMethodArgumentsCacheMode(instance, CacheMode.READ_WRITE);

        String value1 = instance.Random(Boolean.TRUE);
        String value2 = instance.Random(Boolean.TRUE);
        Assert.assertEquals(value1, value2);
        SimpleRulesRuntimeEnvUtils.setMethodArgumentsCacheEnable(instance, false);
        String value3 = instance.Random(Boolean.TRUE);
        Assert.assertNotNull(value1);
        Assert.assertFalse(value1.equals(value3));
    }

    private static class MyRunnable implements Runnable {
        private A1 instance;

        public MyRunnable(A1 instance) {
            this.instance = instance;
        }

        @Override
        public void run() {
            SimpleRulesRuntimeEnvUtils.setMethodArgumentsCacheEnable(instance, true);
            SimpleRulesRuntimeEnvUtils.setMethodArgumentsCacheMode(instance, CacheMode.READ_WRITE);

            String value1 = instance.Random(Boolean.TRUE);
            String value2 = instance.Random(Boolean.TRUE);
            Assert.assertEquals(value1, value2);
        }
    }

    @Test
    public void cacheTestMultithreading() throws Exception {
        Thread t = new Thread(new MyRunnable(instance));
        t.start();
        t.join();
        String value1 = instance.Random(Boolean.TRUE);
        String value2 = instance.Random(Boolean.TRUE);
        Assert.assertFalse(value1.equals(value2));
        Thread t2 = new Thread(new MyRunnable(instance));
        t2.start();
        t2.join();
    }

    @Test
    public void defaultValueTest() {
        Assert.assertFalse(SimpleRulesRuntimeEnvUtils.isMethodArgumentsCacheEnable(instance));
    }

    @Test
    public void skipCacheTest() {
        SimpleRulesRuntimeEnvUtils.setMethodArgumentsCacheEnable(instance, true);
        SimpleRulesRuntimeEnvUtils.setMethodArgumentsCacheMode(instance, CacheMode.READ_WRITE);
        String value1 = instance.Random2(Boolean.TRUE);
        String value2 = instance.Random2(Boolean.TRUE);
        Assert.assertFalse(value1.equals(value2));
    }
}
