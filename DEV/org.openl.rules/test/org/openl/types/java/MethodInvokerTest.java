package org.openl.types.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.OpenLClassLoader;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;

class MethodInvokerTest {

    @Test
    void aMethodCalledOftenIsCalledDirectly() throws Exception {
        var invoker = invoker(Fixture.class, "sum", int.class, int.class);

        assertEquals(5, invoker.invoke(null, new Object[]{2, 3}));
        assertFalse(invoker.isDirect());

        warmUp(invoker, null, 2, 3);
        assertTrue(invoker.isDirect());
        assertEquals(5, invoker.invoke(null, new Object[]{2, 3}));
    }

    @Test
    void anInstanceMethodIsCalledOnItsTarget() throws Exception {
        var invoker = invoker(Fixture.class, "greet", String.class);
        var target = new Fixture();

        warmUp(invoker, target, "Ann");
        assertTrue(invoker.isDirect());
        assertEquals("Hello, Ann", invoker.invoke(target, new Object[]{"Ann"}));
        assertEquals("Hello, null", invoker.invoke(target, new Object[]{null}));
        assertThrows(IllegalArgumentException.class, () -> invoker.invoke("not a fixture", new Object[]{"Ann"}));
        assertThrows(NullPointerException.class, () -> invoker.invoke(null, new Object[]{"Ann"}));
    }

    @Test
    void aVoidMethodGivesNull() throws Exception {
        var invoker = invoker(Fixture.class, "nothing");

        warmUp(invoker, null);
        assertTrue(invoker.isDirect());
        assertNull(invoker.invoke(null, new Object[0]));
        assertNull(invoker.invoke(null, null));
    }

    @Test
    void whatTheMethodThrowsIsTheCauseOfTheFailure() throws Exception {
        var invoker = invoker(Fixture.class, "fail", String.class);

        for (var i = 0; i <= MethodInvoker.HOT_CALLS; i++) {
            var e = assertThrows(InvocationTargetException.class, () -> invoker.invoke(null, new Object[]{"Boom"}));
            var cause = assertInstanceOf(IllegalStateException.class, e.getCause());
            assertEquals("Boom", cause.getMessage());
        }
        assertTrue(invoker.isDirect());
    }

    @Test
    void anyObjectSubclassesAndSubclassArraysAreCalledDirectly() throws Exception {
        var invoker = invoker(Fixture.class, "describe", Object.class, CharSequence.class, Number[].class);
        Object[] args = {42, new StringBuilder("text"), new Integer[]{1, 2}};

        warmUp(invoker, null, args);

        assertTrue(invoker.isDirect());
        assertEquals("42 text 2", invoker.invoke(null, args));
        assertEquals("null text 0", invoker.invoke(null, new Object[]{null, "text", new Long[0]}));
    }

    @Test
    void aDirectCallThatFailsToLinkGivesWayToReflection() throws Exception {
        var invoker = new MethodInvoker(Fixture.class.getMethod("sum", int.class, int.class), new UnlinkedCall());

        assertTrue(invoker.isDirect());
        assertEquals(5, invoker.invoke(null, new Object[]{2, 3}));
        assertFalse(invoker.isDirect());
        assertEquals(5, invoker.invoke(null, new Object[]{2, 3}));
    }

    @Test
    void aLinkageErrorTheMethodThrowsIsItsFailure() throws Exception {
        var invoker = invoker(Fixture.class, "unlinked");

        for (var i = 0; i <= MethodInvoker.HOT_CALLS; i++) {
            var e = assertThrows(InvocationTargetException.class, () -> invoker.invoke(null, new Object[0]));
            assertInstanceOf(NoClassDefFoundError.class, e.getCause());
        }
        assertTrue(invoker.isDirect());
    }

    @Test
    void argumentsOfOtherTypesAreConvertedOrRejectedAsByReflection() throws Exception {
        var invoker = invoker(Fixture.class, "widen", long.class);

        warmUp(invoker, null, 7L);
        assertTrue(invoker.isDirect());
        // Reflection widens an int to a long
        assertEquals(7L, invoker.invoke(null, new Object[]{7}));
        assertThrows(IllegalArgumentException.class, () -> invoker.invoke(null, new Object[]{null}));
        assertThrows(IllegalArgumentException.class, () -> invoker.invoke(null, new Object[]{"7"}));
        assertThrows(IllegalArgumentException.class, () -> invoker.invoke(null, new Object[]{7L, 8L}));
    }

    @Test
    void methodsOfAnInterfaceAreCalled() throws Exception {
        var square = invoker(Shape.class, "square", double.class);
        var area = invoker(Shape.class, "area");

        warmUp(square, null, 3.0);
        var shape = square.invoke(null, new Object[]{3.0});
        warmUp(area, shape);

        assertTrue(square.isDirect());
        assertTrue(area.isDirect());
        assertEquals(9.0, area.invoke(shape, new Object[0]));
    }

    @Test
    void aMethodOfAClassThatIsNotPublicIsCalledByReflection() throws Exception {
        var invoker = invoker(Internal.class, "one");

        warmUp(invoker, null);
        assertFalse(invoker.isDirect());
        assertEquals(1, invoker.invoke(null, new Object[0]));
    }

    @Test
    void aMethodOfAJdkClassIsCalledByReflection() throws Exception {
        var invoker = invoker(String.class, "length");

        warmUp(invoker, "abc");
        assertFalse(invoker.isDirect());
        assertEquals(3, invoker.invoke("abc", new Object[0]));
    }

    @Test
    void aMethodOfAGeneratedBeanIsCalledFromTheBeanClassLoader() throws Exception {
        var className = "org.openl.generated.MethodInvokerBean";
        var loader = new OpenLClassLoader(MethodInvokerTest.class.getClassLoader());
        var byteCode = new JavaBeanClassBuilder(className).addField("amount", new FieldDescription("int")).byteCode();
        var beanClass = ClassLoaderUtils.defineClass(className, byteCode, loader);
        var bean = beanClass.getDeclaredConstructor().newInstance();
        var setter = new MethodInvoker(beanClass.getMethod("setAmount", int.class));
        var getter = new MethodInvoker(beanClass.getMethod("getAmount"));

        warmUp(setter, bean, 42);
        warmUp(getter, bean);

        assertTrue(setter.isDirect());
        assertTrue(getter.isDirect());
        setter.invoke(bean, new Object[]{17});
        assertEquals(17, getter.invoke(bean, new Object[0]));
    }

    private static MethodInvoker invoker(Class<?> type, String name, Class<?>... parameterTypes) throws Exception {
        return new MethodInvoker(type.getMethod(name, parameterTypes));
    }

    private static void warmUp(MethodInvoker invoker, Object target, Object... args) throws Exception {
        for (var i = 0; i < MethodInvoker.HOT_CALLS; i++) {
            invoker.invoke(target, args);
        }
    }

    public static class Fixture {
        public static int sum(int a, int b) {
            return a + b;
        }

        public static long widen(long value) {
            return value;
        }

        public static void nothing() {
            // Nothing to do: the result of a void method is checked
        }

        public static String fail(String message) {
            throw new IllegalStateException(message);
        }

        public static String describe(Object value, CharSequence text, Number[] numbers) {
            return value + " " + text + " " + numbers.length;
        }

        public static void unlinked() {
            throw new NoClassDefFoundError("org/openl/Missing");
        }

        public String greet(String name) {
            return "Hello, " + name;
        }
    }

    /**
     * A direct call that cannot resolve the method it calls, as a class generated against another version would.
     */
    private static final class UnlinkedCall implements BiFunction<Object, Object[], Object> {
        @Override
        public Object apply(Object target, Object[] args) {
            throw new NoSuchMethodError("Fixture.sum(II)I");
        }
    }

    public interface Shape {
        double area();

        static Shape square(double side) {
            return () -> side * side;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    static final class Internal {
        public static int one() {
            return 1;
        }
    }
}
