package org.openl.gen;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.util.ClassUtils;

public class JavaInterfaceImplGeneratorTest {

    @Test
    public void testInterfaceImplGeneration() throws IllegalAccessException, InstantiationException {
        IBean iBean = (IBean) newInstance(IBean.class);
        iBean.setField1("foo");
        iBean.setField2(1);
        assertEquals("foo", iBean.getField1());
        assertEquals(1, iBean.getField2());
        iBean.someMethod();
        iBean.someMethod(new Date(11112L), "pam-pam");
        assertNull(iBean.calculate(new Date(11112L), new Date(11112L)));
        assertFalse(iBean.returnBoolean());
        assertEquals(0, iBean.returnByte());
        assertEquals(0, iBean.returnInt());
        assertEquals(0L, iBean.returnLong());
        assertEquals(0, iBean.returnShort());
        assertEquals(new Float(0F), new Float(iBean.returnFloat()));
        assertEquals(new Double(0D), new Double(iBean.returnDouble()));
        assertEquals(0, iBean.returnChar());
    }

    @Test
    public void testNotPOJOInterfaceImplGeneration() throws InstantiationException, IllegalAccessException {
        newInstance(IEmpty.class);
        newInstance(INotPOJO.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotInterfaceGeneration() {
        new JavaInterfaceImplBuilder(Date.class);
    }

    @Test
    public void testEquals() throws IllegalAccessException, InstantiationException {
        Class<?> clazz = getBeanClass(IBean.class);
        IBean beanA = (IBean) clazz.newInstance();
        IBean beanB = (IBean) clazz.newInstance();
        assertEquals(beanA, beanB);
        beanA.setField1("foo");
        beanA.setField2(1);
        assertNotEquals(beanA, beanB);
        assertNotEquals(beanB, beanA);
        beanB.setField2(1);
        assertNotEquals(beanA, beanB);
        assertNotEquals(beanB, beanA);
        beanB.setField1("foo");
        assertEquals(beanA, beanB);
        assertEquals(beanB, beanA);

        assertEquals(beanA.hashCode(), beanB.hashCode());
    }

    private Object newInstance(Class<?> clazzInterface) throws IllegalAccessException, InstantiationException {
        Class<?> clazz = getBeanClass(clazzInterface);
        assertNotNull(clazz);
        return clazz.newInstance();
    }

    private Class<?> getBeanClass(Class<?> clazzInterface) {
        ClassLoader simpleBundleClassLoader = new OpenLBundleClassLoader(Thread.currentThread().getContextClassLoader());
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(simpleBundleClassLoader);
            JavaInterfaceImplBuilder builder = new JavaInterfaceImplBuilder(clazzInterface);
            byte[] byteCode = builder.byteCode();
            String className = builder.getBeanName();
            return ClassUtils.defineClass(className, byteCode, simpleBundleClassLoader);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public interface IBean extends INotPOJO {
        String getField1();

        void setField1(String field1);

        int getField2();

        void setField2(int field1);

        @Override
        void someMethod();

    }

    public interface IEmpty {

    }

    public interface INotPOJO {
        void someMethod();

        void someMethod(Date d, String f);

        Date calculate(Date d, Date d2);

        int returnInt();

        short returnShort();

        byte returnByte();

        long returnLong();

        float returnFloat();

        double returnDouble();

        char returnChar();

        boolean returnBoolean();
    }
}
