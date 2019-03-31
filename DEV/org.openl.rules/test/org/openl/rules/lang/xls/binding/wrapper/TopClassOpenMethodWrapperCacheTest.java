package org.openl.rules.lang.xls.binding.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.AMethod;
import org.openl.types.java.JavaOpenClassCache;
import org.openl.vm.IRuntimeEnv;

public class TopClassOpenMethodWrapperCacheTest {

    private static void gc() throws Exception {
        System.gc();
        System.gc();
        System.gc();
        Thread.sleep(10);
    }

    @org.junit.Test
    public void test() throws Exception {
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

        gc();
        assertEquals(3, cache.cache.size());
        assertNotNull(openClass1);
        assertNotNull(openClass2);
        assertNotNull(m1);
        assertNotNull(m2);

        // Check cache when a method has dependency on a class
        openClass2 = null;
        gc();
        assertEquals(3, cache.cache.size());
        assertNotNull(openClass1);
        assertNull(openClass2);
        assertNotNull(m1);
        assertNotNull(m2);

        m2 = null;
        gc();
        assertEquals(2, cache.cache.size());
        assertNotNull(openClass1);
        assertNull(openClass2);
        assertNotNull(m1);
        assertNull(m2);

        // Check when a method can be GC-ed, but class is still used
        m1 = null;
        gc();
        assertEquals(2, cache.cache.size());
        assertNotNull(openClass1);
        assertNull(openClass2);
        assertNull(m1);
        assertNull(m2);

        openClass1 = null;
        gc();
        assertEquals(1, cache.cache.size());
        assertNull(openClass1);
        assertNull(openClass2);
        assertNull(m1);
        assertNull(m2);
        assertNotNull(openClass3);
        assertNotNull(m3);

        // Check when a class can be GC-ed, but method is still used
        openClass3 = null;
        gc();
        assertEquals(0, cache.cache.size());
        assertNull(openClass3);
        assertNotNull(m3);

        m3 = null;
        gc();
        assertEquals(0, cache.cache.size());
        assertNull(openClass3);
        assertNull(m3);
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

        private IOpenClass openClass;

        SomeOpenMethod(IOpenClass openClass) {
            super(null);
            this.openClass = openClass;
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
