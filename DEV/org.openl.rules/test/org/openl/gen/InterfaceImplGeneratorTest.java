package org.openl.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;

import org.junit.jupiter.api.Test;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.OpenLClassLoader;
import org.openl.util.ClassUtils;

class InterfaceImplGeneratorTest {

    @Test
    void testInterfaceImplGeneration() throws Exception {
        IBean iBean = newBean(IBean.class);
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
        assertEquals(Float.valueOf(0F), Float.valueOf(iBean.returnFloat()));
        assertEquals(Double.valueOf(0D), Double.valueOf(iBean.returnDouble()));
        assertEquals(0, iBean.returnChar());
    }

    @Test
    void keepsThePropertiesOfEveryInterfaceItExtends() throws Exception {
        var bean = newBean(IGrandChild.class);
        ClassUtils.set(bean, "name", "Ann");
        ClassUtils.set(bean, "age", 30);
        ClassUtils.set(bean, "city", "Kyiv");

        assertEquals("Ann", bean.getName());
        assertEquals(30, bean.getAge());
        assertEquals("Kyiv", bean.getCity());
    }

    @Test
    void answersEveryGetterOfAPropertyWithItsValue() throws Exception {
        var bean = newBean(IFlagged.class);
        ClassUtils.set(bean, "active", true);
        ClassUtils.set(bean, "value", "narrow");

        assertTrue(bean.isActive());
        assertEquals("narrow", bean.getValue());
        assertEquals("narrow", ((IWideValue) bean).getValue());
        assertEquals("narrow", ((INarrowValue) bean).getValue());
    }

    @Test
    void keepsTheDefaultMethodsAndTheMethodsOfObject() throws Exception {
        var bean = newBean(IDocument.class);
        ClassUtils.set(bean, "title", "Guide");

        assertEquals("Document Guide", bean.getFullTitle());
        assertEquals("a document", bean.describe());
        assertTrue(bean.toString().contains("Guide"));
    }

    @Test
    void writesAPropertyThroughASetterOfAWiderType() throws Exception {
        var bean = newBean(INarrowSettable.class);
        ((IWideSettable) bean).setValue("wide");

        assertEquals("wide", bean.getValue());
        assertThrows(ClassCastException.class, () -> ((IWideSettable) bean).setValue(1));
    }

    @Test
    void convertsTheValueBetweenAPrimitiveAndItsWrapper() throws Exception {
        var bean = newBean(IBothFlags.class);
        ((IPrimitiveFlag) bean).setActive(true);

        assertTrue(bean.isActive());
        assertEquals(Boolean.TRUE, bean.getActive());

        ((IBoxedFlag) bean).setActive(null);

        assertFalse(bean.isActive());
        assertNull(bean.getActive());
    }

    @Test
    void refusesAnAccessorOfATypeTheValueDoesNotFit() throws Exception {
        var bean = newBean(ICodes.class);

        var error = assertThrows(UnsupportedOperationException.class, () -> bean.setCode(1));
        assertEquals("The property 'code' is kept as 'java.lang.String'.", error.getMessage());
        assertNull(bean.getCode());
    }

    @Test
    void keepsADefaultMethodThatOverridesAnAbstractOne() throws Exception {
        var bean = newBean(IDefaultTask.class);

        assertEquals("default task", bean.getName());
        assertThrows(IllegalStateException.class, bean::run);
    }

    @Test
    void stubsAMethodThatAnInterfaceNarrows() throws Exception {
        var bean = newBean(ILeaf.class);

        assertNull(bean.copy());
        assertNull(((INode) bean).copy());
    }

    @Test
    void stubsAnIndexedGetter() throws Exception {
        var bean = newBean(ILines.class);
        ClassUtils.set(bean, "name", "lines");

        assertEquals("lines", bean.getName());
        assertNull(bean.getLine(0));
    }

    @Test
    void testNotPOJOInterfaceImplGeneration() throws Exception {
        assertNotNull(newBean(IEmpty.class));
        assertNotNull(newBean(INotPOJO.class));
    }

    @Test
    void testNotInterfaceGeneration() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InterfaceImplBuilder(Date.class);
        });
    }

    @Test
    void testEquals() throws Exception {
        Class<?> clazz = getBeanClass(IBean.class);
        IBean beanA = (IBean) clazz.getDeclaredConstructor().newInstance();
        IBean beanB = (IBean) clazz.getDeclaredConstructor().newInstance();
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

    private static <T> T newBean(Class<T> clazzInterface) throws ReflectiveOperationException {
        return clazzInterface.cast(getBeanClass(clazzInterface).getDeclaredConstructor().newInstance());
    }

    private static Class<?> getBeanClass(Class<?> clazzInterface) {
        ClassLoader simpleClassLoader = new OpenLClassLoader(
                Thread.currentThread().getContextClassLoader());
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(simpleClassLoader);
            InterfaceImplBuilder builder = new InterfaceImplBuilder(clazzInterface);
            byte[] byteCode = builder.byteCode();
            String className = builder.getBeanName();
            return ClassLoaderUtils.defineClass(className, byteCode, simpleClassLoader);
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

    public interface IGrandParent {
        String getName();
    }

    public interface IParent extends IGrandParent {
        int getAge();
    }

    public interface IGrandChild extends IParent {
        String getCity();
    }

    public interface IWideValue {
        Object getValue();
    }

    public interface IWideHolder extends IWideValue {
    }

    public interface INarrowValue {
        String getValue();
    }

    public interface IFlagged extends IWideHolder, INarrowValue {
        boolean isActive();
    }

    public interface ILines {
        String getName();

        String getLine(int index);
    }

    public interface IDocument {
        String getTitle();

        default String getFullTitle() {
            return "Document " + getTitle();
        }

        default String describe() {
            return "a document";
        }

        @Override
        String toString();
    }

    public interface IWideSettable {
        Object getValue();

        void setValue(Object value);
    }

    public interface INarrowSettable extends IWideSettable {
        @Override
        String getValue();
    }

    public interface IBoxedFlag {
        Boolean getActive();

        void setActive(Boolean active);
    }

    public interface IPrimitiveFlag {
        boolean isActive();

        void setActive(boolean active);
    }

    public interface IBothFlags extends IBoxedFlag, IPrimitiveFlag {
    }

    public interface ICodeText {
        String getCode();
    }

    public interface ICodeNumber {
        void setCode(Integer code);
    }

    public interface ICodes extends ICodeText, ICodeNumber {
    }

    public interface ITask {
        String getName();

        void run();
    }

    public interface IDefaultTask extends ITask {
        @Override
        default String getName() {
            return "default task";
        }

        @Override
        default void run() {
            throw new IllegalStateException("The default method runs.");
        }
    }

    public interface INode {
        INode copy();
    }

    public interface IMid extends INode {
    }

    public interface ILeaf extends IMid {
        @Override
        ILeaf copy();
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
