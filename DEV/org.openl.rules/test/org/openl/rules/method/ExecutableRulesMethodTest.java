package org.openl.rules.method;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.CacheMode;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;

public class ExecutableRulesMethodTest {

    private static String __src = "test/rules/MethodArgumentsCacheTest.xls";

    private RulesEngineFactory<A1> engineFactory;
    private A1 instance;

    public interface A1 {
        String Random(Boolean a1);

        String Random2(Boolean a1);
    }

    @Before
    public void init() throws IOException {
        engineFactory = new RulesEngineFactory<>(__src, A1.class);
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

    final static class SimpleRulesRuntimeEnvUtils {
        private SimpleRulesRuntimeEnvUtils() {
        }

        public static IRuntimeEnv getRuntimeEnv(Object instance) {
            if (instance instanceof IEngineWrapper) {
                IRuntimeEnv runtimeEnv;
                if (instance instanceof IEngineWrapper) {
                    runtimeEnv = ((IEngineWrapper) instance).getRuntimeEnv();
                } else {
                    try {
                        runtimeEnv = (IRuntimeEnv) instance.getClass()
                            .getMethod("getRuntimeEnvironment")
                            .invoke(instance);
                    } catch (Exception e) {
                        throw new OpenlNotCheckedException(e);
                    }
                }
                return runtimeEnv;
            }
            throw new OpenlNotCheckedException(
                String.format("Expected an Instance of interface '%s'.", IEngineWrapper.class.getTypeName()));
        }

        public static boolean isMethodArgumentsCacheEnable(Object instance) {
            IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
            return isMethodArgumentsCacheEnableEnv(runtimeEnv);
        }

        public static CacheMode getMethodArgumentsCacheMode(Object instance) {
            IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
            return getMethodArgumentsCacheModeEnv(runtimeEnv);
        }

        public static boolean isCacheSupports(Object instance) {
            IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
            return isCacheSupportsEnv(runtimeEnv);
        }

        public static void setMethodArgumentsCacheMode(Object instance, CacheMode cacheMode) {
            IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
            changeMethodArgumentsCacheEnv(runtimeEnv, cacheMode);
        }

        public static void setMethodArgumentsCacheEnable(Object instance, boolean enable) {
            IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
            setMethodArgumentsCacheEnableEnv(runtimeEnv, enable);
        }

        private static boolean isCacheSupportsEnv(IRuntimeEnv env) {
            return env instanceof SimpleRulesRuntimeEnv;
        }

        private static void setMethodArgumentsCacheEnableEnv(IRuntimeEnv env, boolean enable) {
            if (isCacheSupportsEnv(env)) {
                ((SimpleRulesRuntimeEnv) env).setMethodArgumentsCacheEnable(enable);
                return;
            }
            throw new OpenlNotCheckedException("Runtime env does not support cache.");
        }

        private static boolean isMethodArgumentsCacheEnableEnv(IRuntimeEnv env) {
            if (isCacheSupportsEnv(env)) {
                return ((SimpleRulesRuntimeEnv) env).isMethodArgumentsCacheEnable();
            }
            throw new OpenlNotCheckedException("Runtime env does not support cache.");
        }

        private static CacheMode getMethodArgumentsCacheModeEnv(IRuntimeEnv env) {
            if (isCacheSupportsEnv(env)) {
                return ((SimpleRulesRuntimeEnv) env).getCacheMode();
            }
            throw new OpenlNotCheckedException("Runtime env does not support cache.");
        }

        private static void changeMethodArgumentsCacheEnv(IRuntimeEnv env, CacheMode cacheMode) {
            if (isCacheSupportsEnv(env)) {
                ((SimpleRulesRuntimeEnv) env).changeMethodArgumentsCacheMode(cacheMode);
                return;
            }
            throw new OpenlNotCheckedException("Runtime env does not support cache.");
        }
    }

}
