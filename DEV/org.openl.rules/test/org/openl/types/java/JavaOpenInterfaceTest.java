package org.openl.types.java;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.Test;

import org.openl.classloader.OpenLClassLoader;
import org.openl.types.IOpenField;
import org.openl.vm.SimpleRuntimeEnv;

class JavaOpenInterfaceTest {

    private final SimpleRuntimeEnv env = new SimpleRuntimeEnv();

    @Test
    void fillsAPropertyWithoutASetterOnTheBean() {
        var type = openInterface(Shape.class);
        var field = fieldToFill(type, "name");

        assertTrue(field.isWritable());
        assertTrue(field.isReadable());
        assertEquals("name", field.getName());
        assertEquals(JavaOpenClass.STRING, field.getType());

        var bean = type.newInstance(env);
        field.set(bean, "square", env);

        assertEquals("square", ((Shape) bean).getName());
        assertEquals("square", field.get(bean, env));
    }

    @Test
    void fillsAPropertyThatTheInterfaceNarrows() {
        var type = openInterface(NarrowValue.class);
        var field = fieldToFill(type, "value");
        var bean = type.newInstance(env);
        field.set(bean, "narrow", env);

        assertTrue(field.isWritable());
        assertEquals("narrow", ((NarrowValue) bean).getValue());
        assertEquals("narrow", ((WideValue) bean).getValue());
    }

    @Test
    void fillsAPropertyOfTwoInterfacesWithTheNarrowestType() {
        var type = openInterface(BothNumbers.class);
        var field = fieldToFill(type, "value");
        var bean = type.newInstance(env);
        field.set(bean, 30, env);

        assertEquals(JavaOpenClass.getOpenClass(Integer.class), field.getType());
        assertEquals(30, ((NarrowNumber) bean).getValue());
        assertEquals(30, ((WideValue) bean).getValue());
    }

    @Test
    void fillsAPropertyOfAGenericInterfaceWithTheTypeRulesRead() {
        var type = openInterface(Account.class);
        var field = fieldToFill(type, "id");
        var bean = type.newInstance(env);
        field.set(bean, 7L, env);

        assertEquals(type.getField("id").getType(), field.getType());
        assertEquals(7L, ((Account) bean).getId());
    }

    @Test
    void buildsOneBeanClassForAnInterface() {
        var bean = openInterface(Shape.class).newInstance(env);

        assertSame(bean.getClass(), new JavaOpenInterface(Shape.class).newInstance(env).getClass());
    }

    @Test
    void readsThePropertyOfAnyObjectOfTheInterface() {
        var type = openInterface(Shape.class);
        var field = fieldToFill(type, "sides");

        assertEquals(0, field.get(new Circle(), env));
    }

    @Test
    void keepsTheFieldsATableFillsAlready() {
        var type = openInterface(Shape.class);
        var withSetter = type.getField("color");
        var constant = type.getField("KIND");

        assertSame(withSetter, type.getFieldToFill(withSetter));
        assertSame(constant, type.getFieldToFill(constant));
    }

    @Test
    void keepsTheFieldsOfACollectionAndOfAHiddenInterface() {
        var list = openInterface(List.class);
        var empty = list.getField("empty");
        var hidden = openInterface(Hidden.class);
        var name = hidden.getField("name");

        assertSame(empty, list.getFieldToFill(empty));
        assertSame(name, hidden.getFieldToFill(name));
        assertFalse(hidden.getFieldToFill(name).isWritable());
    }

    @Test
    void refusesToBuildAnInterfaceThatIsNotPublic() {
        var hidden = openInterface(Hidden.class);

        var error = assertThrows(IllegalStateException.class, () -> hidden.newInstance(env));
        assertEquals("""
                Cannot create an object of the interface '%s': it is sealed, not public, in a package its module\
                 does not export, or a collection the JDK does not implement.""".formatted(Hidden.class.getTypeName()),
                error.getMessage());
    }

    @Test
    void refusesToBuildASealedInterface() {
        var sealed = openInterface(SealedShape.class);

        assertThrows(IllegalStateException.class, () -> sealed.newInstance(env));
    }

    @Test
    void refusesToSetAPropertyOnAnotherObjectOfTheInterface() {
        var type = openInterface(Shape.class);
        var field = fieldToFill(type, "name");
        var circle = new Circle();

        var error = assertThrows(IllegalStateException.class, () -> field.set(circle, "square", env));
        assertEquals("Cannot set the property 'name' of '%s', which has no setter of it."
                .formatted(Circle.class.getTypeName()), error.getMessage());
    }

    @Test
    void setsNothingOnNoObject() {
        var field = fieldToFill(openInterface(Shape.class), "name");

        assertDoesNotThrow(() -> field.set(null, "square", env));
    }

    @Test
    void tellsTheInterfacesThatMayOutliveTheEngine() throws Exception {
        var location = JavaOpenInterfaceTest.class.getProtectionDomain().getCodeSource().getLocation();
        try (var application = new URLClassLoader(new URL[]{location}, ClassLoader.getPlatformClassLoader());
             var isolated = new OpenLClassLoader(null)) {
            var foreign = Class.forName(SharedShape.class.getName(), false, application);
            var ofOpenL = isolated.parseClass("interface Isolated { String getName() }");

            assertTrue(JavaOpenInterface.mayOutliveEngine(Runnable.class));
            assertTrue(JavaOpenInterface.mayOutliveEngine(Class.forName("java.sql.Wrapper")));
            assertTrue(JavaOpenInterface.mayOutliveEngine(foreign));
            assertFalse(JavaOpenInterface.mayOutliveEngine(SharedShape.class));
            assertFalse(JavaOpenInterface.mayOutliveEngine(ofOpenL));
            assertInstanceOf(foreign, openInterface(foreign).newInstance(env));
        }
    }

    @Test
    void refusesToBuildAnInterfaceOfAPackageThatIsNotExported() throws Exception {
        var internal = openInterface(Class.forName("jdk.internal.access.JavaLangAccess"));

        assertThrows(IllegalStateException.class, () -> internal.newInstance(env));
    }

    @Test
    void keepsTheBeanOfAnInterfaceOfTheJdkWithTheEngine() {
        var entry = openInterface(Map.Entry.class);
        var key = fieldToFill(entry, "key");
        var bean = entry.newInstance(env);
        key.set(bean, "code", env);

        assertEquals("code", ((Map.Entry<?, ?>) bean).getKey());
        assertSame(ClassLoader.getPlatformClassLoader(), bean.getClass().getClassLoader().getParent());
        assertSame(bean.getClass(), new JavaOpenInterface(Map.Entry.class).newInstance(env).getClass());
    }

    @Test
    void fillsAPropertyWithAGetterAndAnIsGetter() {
        var type = openInterface(Flag.class);
        var field = fieldToFill(type, "active");
        var bean = type.newInstance(env);
        field.set(bean, Boolean.TRUE, env);

        assertTrue(field.isWritable());
        assertEquals(JavaOpenClass.getOpenClass(Boolean.class), field.getType());
        assertTrue(((Flag) bean).isActive());
        assertEquals(Boolean.TRUE, ((Flag) bean).getActive());
    }

    @Test
    void keepsAPropertyADefaultGetterComputes() {
        var type = openInterface(Titled.class);
        var fullTitle = type.getField("fullTitle");

        assertSame(fullTitle, type.getFieldToFill(fullTitle));
    }

    @Test
    void definesTheBeanOfAnInterfaceOfAProjectInTheProject() throws Exception {
        var project = new OpenLClassLoader(getClass().getClassLoader());
        var type = project.parseClass("interface ProjectShape { String getName() }");
        var bean = openInterface(type).newInstance(env);

        assertInstanceOf(type, bean);
        assertSame(project, bean.getClass().getClassLoader());
        assertSame(bean.getClass(), Class.forName(bean.getClass().getName(), false, project));
    }

    @Test
    void keepsTheBeanOfAnInterfaceOfTheEngineUnderItsClassLoader() {
        var bean = openInterface(SharedShape.class).newInstance(env);
        var beanLoader = bean.getClass().getClassLoader();

        assertInstanceOf(SharedShape.class, bean);
        assertInstanceOf(OpenLClassLoader.class, beanLoader);
        assertSame(SharedShape.class.getClassLoader(), beanLoader.getParent());
        assertSame(bean.getClass(), openInterface(SharedShape.class).newInstance(env).getClass());
    }

    @Test
    void buildsACollectionOfTheInterface() {
        assertInstanceOf(ArrayList.class, openInterface(List.class).newInstance(env));
        assertInstanceOf(HashSet.class, openInterface(Set.class).newInstance(env));
        assertInstanceOf(TreeMap.class, openInterface(SortedMap.class).newInstance(env));
        assertInstanceOf(HashMap.class, openInterface(Map.class).newInstance(env));
        assertInstanceOf(ArrayList.class, openInterface(Collection.class).newInstance(env));
        assertInstanceOf(TreeSet.class, openInterface(NavigableSet.class).newInstance(env));
        assertInstanceOf(LinkedHashSet.class, openInterface(SequencedSet.class).newInstance(env));
        assertInstanceOf(ArrayDeque.class, openInterface(Queue.class).newInstance(env));
        assertInstanceOf(ArrayDeque.class, openInterface(Deque.class).newInstance(env));
        assertInstanceOf(LinkedHashMap.class, openInterface(SequencedMap.class).newInstance(env));
        assertInstanceOf(TreeMap.class, openInterface(NavigableMap.class).newInstance(env));
        assertInstanceOf(ConcurrentHashMap.class, openInterface(ConcurrentMap.class).newInstance(env));
        assertInstanceOf(ConcurrentSkipListMap.class, openInterface(ConcurrentNavigableMap.class).newInstance(env));
    }

    @Test
    void buildsABeanOfAnInterfaceACollectionImplements() {
        var iterable = openInterface(Iterable.class).newInstance(env);

        assertInstanceOf(Iterable.class, iterable);
        assertFalse(iterable instanceof Collection);
    }

    private static IOpenField fieldToFill(JavaOpenInterface type, String name) {
        return type.getFieldToFill(type.getField(name));
    }

    private static JavaOpenInterface openInterface(Class<?> type) {
        return assertInstanceOf(JavaOpenInterface.class, JavaOpenClass.getOpenClass(type));
    }

    public interface Shape {

        String KIND = "shape";

        String getName();

        int getSides();

        String getColor();

        void setColor(String color);
    }

    interface Hidden {

        String getName();
    }

    public interface SharedShape {

        String getName();
    }

    public sealed interface SealedShape permits Square {

        String getName();
    }

    record Square(String getName) implements SealedShape {
    }

    public interface Flag {

        Boolean getActive();

        boolean isActive();
    }

    public interface Titled {

        String getTitle();

        default String getFullTitle() {
            return "Title: " + getTitle();
        }
    }

    public interface WideValue {

        Object getValue();
    }

    public interface NarrowValue extends WideValue {

        @Override
        String getValue();
    }

    public interface NarrowNumber {

        Integer getValue();
    }

    public interface BothNumbers extends WideValue, NarrowNumber {
    }

    public interface Identified<I> {

        I getId();
    }

    public interface Account extends Identified<Long> {
    }

    static final class Circle implements Shape {

        @Override
        public String getName() {
            return "circle";
        }

        @Override
        public int getSides() {
            return 0;
        }

        @Override
        public String getColor() {
            return "red";
        }

        @Override
        public void setColor(String color) {
            throw new UnsupportedOperationException();
        }
    }
}
