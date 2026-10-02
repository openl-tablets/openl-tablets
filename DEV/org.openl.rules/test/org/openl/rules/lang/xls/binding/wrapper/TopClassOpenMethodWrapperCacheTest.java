package org.openl.rules.lang.xls.binding.wrapper;

import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.ref.Reference;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.AMethod;
import org.openl.types.java.JavaOpenClassCache;
import org.openl.vm.IRuntimeEnv;

class TopClassOpenMethodWrapperCacheTest {

    private static final int AWAIT_TIMEOUT = 60;

    @Test
    void test() {
        // The test keeps its own references in arrays: clearing an element is what makes its object unreachable.
        var classes = new SomeOpenClass[]{new SomeOpenClass("Class1"),
                new SomeOpenClass("Class2"),
                new SomeOpenClass("Class3")};
        var methods = new SomeOpenMethod[]{new SomeOpenMethod(), new SomeOpenMethod(), new SomeOpenMethod()};

        var cache = new TopClassOpenMethodWrapperCache(null);
        for (var i = 0; i < classes.length; i++) {
            cache.put(classes[i], methods[i]);
        }

        JavaOpenClassCache.getInstance().resetClassloader(Thread.currentThread().getContextClassLoader());

        // Initial test
        assertEquals(3, cache.cache.size());
        awaitCacheSize(cache, 3, classes, methods);

        // Check cache when a method has dependency on a class
        // There is no reason to keep Class2 in the cache, if no reference exists to the key.
        // Zulu JVM cleans weak references eager.
        classes[1] = null;
        methods[1] = null;
        awaitCacheSize(cache, 2, classes, methods);

        // Check when a method can be GC-ed, but class is still used
        methods[0] = null;
        awaitCacheSize(cache, 2, classes, methods);

        classes[0] = null;
        awaitCacheSize(cache, 1, classes, methods);

        // Check when a class can be GC-ed, but method is still used
        classes[2] = null;
        awaitCacheSize(cache, 0, classes, methods);

        methods[2] = null;
        awaitCacheSize(cache, 0, classes, methods);
    }

    /**
     * Waits until the cache holds the expected number of entries. The given objects stay strongly reachable until the
     * wait is over, so the garbage collector can clear only what the test has already released.
     */
    private static void awaitCacheSize(TopClassOpenMethodWrapperCache cache, int expected, Object... strongRefs) {
        given().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(getCacheSize(cache), equalTo(expected));
        Reference.reachabilityFence(strongRefs);
    }

    private static Callable<Integer> getCacheSize(TopClassOpenMethodWrapperCache cache) {
        return () -> {
            System.gc();
            return cache.cache.size();
        };
    }

    private static class SomeOpenClass extends ADynamicClass {

        SomeOpenClass(String className) {
            super(className, null);
        }

        @Override
        public Object newInstance(IRuntimeEnv env) {
            return null;
        }
    }

    private static class SomeOpenMethod extends AMethod {

        SomeOpenMethod() {
            super(null);
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isConstructor() {
            throw new UnsupportedOperationException();
        }
    }
}
