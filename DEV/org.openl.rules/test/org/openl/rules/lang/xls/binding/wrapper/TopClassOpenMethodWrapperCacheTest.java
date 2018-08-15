package org.openl.rules.lang.xls.binding.wrapper;

import org.junit.Assert;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class TopClassOpenMethodWrapperCacheTest {

    public static final class Test {
        public void m1() {
        }

        public void m2() {
        }
    }

    public static final class Test2 {
        public void m1() {
        }

        public void m2() {
        }
    }

    @org.junit.Test
    public void test() throws Exception {
        IOpenClass openClass = JavaOpenClass.getOpenClass(Test.class);
        IOpenClass openClass2 = JavaOpenClass.getOpenClass(Test2.class);

        TopClassOpenMethodWrapperCache cache = new TopClassOpenMethodWrapperCache(null);
        cache.put(openClass, openClass.getMethod("m1", new IOpenClass[] {}));

        // IOpenMethod m2 = openClass2.getMethod("m2", new IOpenClass[] {});
        IOpenMethod m2w = new IOpenMethodW(openClass2);
        cache.put(openClass2, m2w);

        JavaOpenClass.resetClassloader(Thread.currentThread().getContextClassLoader());

        Assert.assertEquals(2, cache.cache.size());
        gc();
        Assert.assertEquals(2, cache.cache.size());

        openClass2 = null;
        gc();
        Assert.assertEquals(2, cache.cache.size());

        m2w = null;
        gc();
        Assert.assertEquals(1, cache.cache.size());

        Assert.assertNull(m2w);
    }

    private void gc() throws InterruptedException {
        System.gc();
        System.gc();
        System.gc();
        Thread.sleep(10);
    }

    private static class IOpenMethodW implements IOpenMethod {

        public IOpenMethodW(IOpenClass openClass) {
            this.openClass = openClass;
        }

        private IOpenClass openClass;

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IOpenMethod getMethod() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDisplayName(int mode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isStatic() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IOpenClass getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IMemberMetaInfo getInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return openClass;
        }

        @Override
        public IMethodSignature getSignature() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isConstructor() {
            throw new UnsupportedOperationException();
        }
    }
}
