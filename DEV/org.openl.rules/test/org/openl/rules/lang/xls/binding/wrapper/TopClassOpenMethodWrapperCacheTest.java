package org.openl.rules.lang.xls.binding.wrapper;

import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.AMethod;
import org.openl.types.java.JavaOpenClassCache;
import org.openl.vm.IRuntimeEnv;

public class TopClassOpenMethodWrapperCacheTest {

    private static final int AWAIT_TIMEOUT = 60;

    @Test
    public void test() {
        IOpenClass openClass1 = new SomeOpenClass("Class1");
        IOpenMethod m1 = new SomeOpenMethod(null);

        IOpenClass openClass2 = new SomeOpenClass("Class2");
        IOpenMethod m2 = new SomeOpenMethod(openClass2);

        IOpenClass openClass3 = new SomeOpenClass("Class3");
        IOpenMethod m3 = new SomeOpenMethod(null);

        TopClassOpenMethodWrapperCache cache = new TopClassOpenMethodWrapperCache(null);
        cache.put(openClass1, m1);
        cache.put(openClass2, m2);
        cache.put(openClass3, m3);

        JavaOpenClassCache.getInstance().resetClassloader(Thread.currentThread().getContextClassLoader());

        // Initial test
        assertEquals(3, cache.cache.size());
        assertNotNull(openClass1);
        assertNotNull(openClass2);
        assertNotNull(openClass3);
        assertNotNull(m1);
        assertNotNull(m2);
        assertNotNull(m3);

        given().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(getCacheSize(cache), equalTo(3));
        assertNotNull(openClass1);
        assertNotNull(openClass2);
        assertNotNull(m1);
        assertNotNull(m2);

        // Check cache when a method has dependency on a class
        openClass2 = null;
        given().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(getCacheSize(cache), equalTo(3));
        assertNotNull(openClass1);
        assertNull(openClass2);
        assertNotNull(m1);
        assertNotNull(m2);

        m2 = null;
        given().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(getCacheSize(cache), equalTo(2));
        assertNotNull(openClass1);
        assertNull(openClass2);
        assertNotNull(m1);
        assertNull(m2);

        // Check when a method can be GC-ed, but class is still used
        m1 = null;
        given().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(getCacheSize(cache), equalTo(2));
        assertNotNull(openClass1);
        assertNull(openClass2);
        assertNull(m1);
        assertNull(m2);

        openClass1 = null;
        given().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(getCacheSize(cache), equalTo(1));
        assertNull(openClass1);
        assertNull(openClass2);
        assertNull(m1);
        assertNull(m2);
        assertNotNull(openClass3);
        assertNotNull(m3);

        // Check when a class can be GC-ed, but method is still used
        openClass3 = null;
        given().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(getCacheSize(cache), equalTo(0));
        assertNull(openClass3);
        assertNotNull(m3);

        m3 = null;
        given().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(getCacheSize(cache), equalTo(0));
        assertNull(openClass3);
        assertNull(m3);
    }

    private Callable<Integer> getCacheSize(TopClassOpenMethodWrapperCache cache) {
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

        SomeOpenMethod(IOpenClass openClass) {
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
