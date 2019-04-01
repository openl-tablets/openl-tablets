package org.openl.rules.dt;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestUtils;

public class DTTest {
    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create("test/rules/dt/DTTest.xlsx");
    }

    @Test
    public void greeting() {
        Object result = TestUtils.invoke(instance, "Greeting", 0);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 11);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 12);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 15);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 17);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 18);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 20);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 21);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 22);
        assertEquals("Good Night, World!", result);
        result = TestUtils.invoke(instance, "Greeting", 23);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greeting7() {
        String[] result = TestUtils.invoke(instance, "Greeting7", 0);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 11);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 12);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 15);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 17);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 18);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 20);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 21);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 22);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting7", 23);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
    }

    @Test
    public void greeting70() {
        String result = TestUtils.invoke(instance, "Greeting70", 0);
        assertEquals("Good Evening", result);
    }

    @Test
    public void greeting8() {
        String[] result = TestUtils.invoke(instance, "Greeting8", 0);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 11);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 12);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 15);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 17);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 18);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 20);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 21);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 22);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting8", 23);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
    }

    @Test
    public void greeting9() {
        String[] result = TestUtils.invoke(instance, "Greeting9", 0);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 11);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 12);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 15);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 17);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 18);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 20);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 21);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 22);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting9", 23);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
    }

    @Test
    public void greeting10() {
        Object[] result = TestUtils.invoke(instance, "Greeting10", 0);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 11);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 12);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 15);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 17);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 18);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 20);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 21);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 22);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting10", 23);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
    }

    @Test
    public void greeting11() {
        Object[] result = TestUtils.invoke(instance, "Greeting11", 0);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 11);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 12);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 15);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 17);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 18);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 20);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 21);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 22);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
        result = TestUtils.invoke(instance, "Greeting11", 23);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
    }

    private static Collection<Object> buildCollection(Object... values) {
        Collection<Object> ret = new ArrayList<>();
        Collections.addAll(ret, values);
        return ret;
    }

    private static Set<Object> buildSet(Object... values) {
        Set<Object> ret = new HashSet<>();
        Collections.addAll(ret, values);
        return ret;
    }

    @Test
    public void greeting12() {
        Collection result = result = TestUtils.invoke(instance, "Greeting12", 0);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 11);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 12);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 15);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 17);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 18);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 20);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 21);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 22);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting12", 23);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting15() {
        Collection result = TestUtils.invoke(instance, "Greeting15", 0);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 11);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 12);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 15);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 17);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 18);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 20);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 21);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 22);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting15", 23);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting20() {
        Collection result = TestUtils.invoke(instance, "Greeting20", 0);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 11);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 12);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 15);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 17);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 18);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 20);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 21);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 22);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting20", 23);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting21() {
        Collection result = TestUtils.invoke(instance, "Greeting21", 0);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 11);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 12);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 15);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 17);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 18);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 20);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 21);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 22);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting21", 23);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting22() {
        Collection result = TestUtils.invoke(instance, "Greeting22", 0);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 11);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 12);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 15);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 17);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 18);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 20);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 21);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 22);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting22", 23);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting23() {
        Collection result = TestUtils.invoke(instance, "Greeting23", 0);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 11);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 12);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 15);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 17);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 18);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 20);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 21);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 22);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting23", 23);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting16() {
        Set result = TestUtils.invoke(instance, "Greeting16", 0);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 11);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 12);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 15);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 17);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 18);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 20);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 21);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 22);
        assertEquals(buildSet("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting16", 23);
        assertEquals(buildSet("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting17() {
        Set result = TestUtils.invoke(instance, "Greeting17", 0);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 11);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 12);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 15);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 17);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 18);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 20);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 21);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 22);
        assertEquals(buildSet("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting17", 23);
        assertEquals(buildSet("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting18() {
        Set result = TestUtils.invoke(instance, "Greeting18", 0);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 11);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 12);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 15);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 17);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 18);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 20);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 21);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 22);
        assertEquals(buildSet("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting18", 23);
        assertEquals(buildSet("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting19() {
        Set result = TestUtils.invoke(instance, "Greeting19", 0);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 11);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 12);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 15);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 17);
        assertEquals(buildSet("Good Morning, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 18);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 20);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 21);
        assertEquals(buildSet("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 22);
        assertEquals(buildSet("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting19", 23);
        assertEquals(buildSet("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting14() {
        Collection result = TestUtils.invoke(instance, "Greeting14", 0);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 11);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 12);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 15);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 17);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 18);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 20);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 21);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 22);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting14", 23);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greeting13() {
        Collection result = TestUtils.invoke(instance, "Greeting13", 0);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 11);
        assertEquals(buildCollection("Good Morning, World!", "Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 12);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 15);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 17);
        assertEquals(buildCollection("Good Afternoon, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 18);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 20);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 21);
        assertEquals(buildCollection("Good Evening, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 22);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
        result = TestUtils.invoke(instance, "Greeting13", 23);
        assertEquals(buildCollection("Good Evening, World!", "Good Night, World!"), result);
    }

    @Test
    public void greetingTwoRet3() {
        String[] result = TestUtils.invoke(instance, "GreetingTwoRet3", 0);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 11);
        assertArrayEquals(new String[] { "Good Morning, World!", "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 12);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 15);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 17);
        assertArrayEquals(new String[] { "Good Afternoon, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 18);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 20);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 21);
        assertArrayEquals(new String[] { "Good Evening, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 22);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
        result = TestUtils.invoke(instance, "GreetingTwoRet3", 23);
        assertArrayEquals(new String[] { "Good Evening, World!", "Good Night, World!" }, result);
    }

    @Test
    public void greeting1() {
        Object result = TestUtils.invoke(instance, "Greeting1", 0);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 11);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 12);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 15);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 17);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 18);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 20);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 21);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 22);
        assertEquals("Good Night, World!", result);
        result = TestUtils.invoke(instance, "Greeting1", 23);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greeting2() {
        Object result = TestUtils.invoke(instance, "Greeting2", 0);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 11);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 12);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 15);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 17);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 18);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 20);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 21);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 22);
        assertEquals("Good Night, World!", result);
        result = TestUtils.invoke(instance, "Greeting2", 23);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greeting3() {
        Object result = TestUtils.invoke(instance, "Greeting3", 0);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 11);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 12);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 15);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 17);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 18);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 20);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 21);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 22);
        assertEquals("Good Night, World!", result);
        result = TestUtils.invoke(instance, "Greeting3", 23);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greeting4() {
        Object result = TestUtils.invoke(instance, "Greeting4", 0);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 11);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 12);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 15);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 17);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 18);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 20);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 21);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 22);
        assertEquals("Good Night, World!", result);
        result = TestUtils.invoke(instance, "Greeting4", 23);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void Greeting6() {
        Object result = TestUtils.invoke(instance, "Greeting6", 0);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 11);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 12);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 15);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 17);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 18);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 20);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 21);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 22);
        assertEquals("Good Night, World!", result);
        result = TestUtils.invoke(instance, "Greeting6", 23);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greetingTwoRet2() {
        Object result = TestUtils.invoke(instance, "GreetingTwoRet2", 0);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 11);
        assertEquals("Good Morning, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 12);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 15);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 17);
        assertEquals("Good Afternoon, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 18);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 20);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 21);
        assertEquals("Good Evening, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 22);
        assertEquals("Good Night, World!", result);
        result = TestUtils.invoke(instance, "GreetingTwoRet2", 23);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void driverPremium1() {
        Object result = TestUtils.invoke(instance, "DriverPremium1", "Young Driver", "Married");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremium1", "Young Driver", "Single");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremium1", "Senior Driver", "Married");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremium1", "Senior Driver", "Single");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremium1", "Standard Driver", "");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium1", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremiumTwoRet1() {
        Object result = TestUtils.invoke(instance, "DriverPremiumTwoRet1", "Young Driver", "Married");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremiumTwoRet1", "Young Driver", "Single");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremiumTwoRet1", "Senior Driver", "Married");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremiumTwoRet1", "Senior Driver", "Single");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremiumTwoRet1", "Standard Driver", "");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremiumTwoRet1", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremium2() {
        Object result = TestUtils.invoke(instance, "DriverPremium2", "Young Driver", "Married");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremium2", "Young Driver", "Single");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremium2", "Senior Driver", "Married");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremium2", "Senior Driver", "Single");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremium2", "Standard Driver", "");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium2", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremium102() {
        Object result = TestUtils.invoke(instance, "DriverPremium102", "Young Driver", "Married");
        assertEquals("R1: 700", result);
        result = TestUtils.invoke(instance, "DriverPremium102", "Young Driver", "Single");
        assertEquals("R2: 720", result);
        result = TestUtils.invoke(instance, "DriverPremium102", "Senior Driver", "Married");
        assertEquals("R3: 300", result);
        result = TestUtils.invoke(instance, "DriverPremium102", "Senior Driver", "Single");
        assertEquals("R4: 350", result);
        result = TestUtils.invoke(instance, "DriverPremium102", "Standard Driver", "");
        assertEquals("R5: 500", result);
        result = TestUtils.invoke(instance, "DriverPremium102", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremium3() {
        Object result = TestUtils.invoke(instance, "DriverPremium3", "Young Driver", "Married");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremium3", "Young Driver", "Single");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremium3", "Senior Driver", "Married");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremium3", "Senior Driver", "Single");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremium3", "Standard Driver", "");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium3", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremium7() {
        Object result = TestUtils.invoke(instance, "DriverPremium7", "Married", "Young Driver");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremium7", "Single", "Young Driver");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremium7", "Married", "Senior Driver");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremium7", "Single", "Senior Driver");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremium7", "", "Standard Driver");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium7", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremium4() {
        Object result = TestUtils.invoke(instance, "DriverPremium4", "Young Driver", "Married");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremium4", "Young Driver", "Single");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremium4", "Senior Driver", "Married");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremium4", "Senior Driver", "Single");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremium4", "Standard Driver", "Single");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium4", "Standard Driver", "Married");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium4", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremium5() {
        Object result = TestUtils.invoke(instance, "DriverPremium5", "Young Driver", "Married");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremium5", "Young Driver", "Single");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremium5", "Senior Driver", "Married");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremium5", "Senior Driver", "Single");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremium5", "Standard Driver", "Single");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium5", "Standard Driver", "Married");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium5", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremium6() {
        Object result = TestUtils.invoke(instance, "DriverPremium6", "Young Driver", "", "Married");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremium6", "Young Driver", "", "Single");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremium6", "Senior Driver", "", "Married");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremium6", "Senior Driver", "", "Single");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremium6", "Standard Driver", "", "Single");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium6", "Standard Driver", "", "Married");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium6", "", "", "");
        assertNull(result);
    }

    @Test
    public void driverPremium8() {
        Object result = TestUtils.invoke(instance, "DriverPremium8", "", "Married", "Young Driver");
        assertEquals(new DoubleValue(700), result);
        result = TestUtils.invoke(instance, "DriverPremium8", "", "Single", "Young Driver");
        assertEquals(new DoubleValue(720), result);
        result = TestUtils.invoke(instance, "DriverPremium8", "", "Married", "Senior Driver");
        assertEquals(new DoubleValue(300), result);
        result = TestUtils.invoke(instance, "DriverPremium8", "", "Single", "Senior Driver");
        assertEquals(new DoubleValue(350), result);
        result = TestUtils.invoke(instance, "DriverPremium8", "", "Single", "Standard Driver");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium8", "", "Married", "Standard Driver");
        assertEquals(new DoubleValue(500), result);
        result = TestUtils.invoke(instance, "DriverPremium8", "", "", "");
        assertNull(result);
    }

    @Test
    public void carPrice() {
        Object result = TestUtils.invoke(instance, "CarPrice", "USA", "BMW", "Z4 sDRIVE35i");
        assertEquals(new DoubleValue(55150), result);
        result = TestUtils.invoke(instance, "CarPrice", "Belarus", "Porche", "911 Carrera 4");
        assertEquals(new DoubleValue(130030), result);
        result = TestUtils.invoke(instance, "CarPrice", "", "", "");
        assertNull(result);
    }

    @Test
    public void carPrice2() {
        Object result = TestUtils.invoke(instance, "CarPrice2", "", "USA", "BMW", "Z4 sDRIVE35i");
        assertEquals(new DoubleValue(55150), result);
        result = TestUtils.invoke(instance, "CarPrice2", "", "Belarus", "Porche", "911 Carrera 4");
        assertEquals(new DoubleValue(130030), result);
        result = TestUtils.invoke(instance, "CarPrice2", "", "", "", "");
        assertNull(result);
    }

    @Test
    public void carPrice3() {
        Object result = TestUtils.invoke(instance, "CarPrice3", "", "BMW", "Z4 sDRIVE35i", "USA");
        assertEquals(new DoubleValue(55150), result);
        result = TestUtils.invoke(instance, "CarPrice3", "", "Porche", "911 Carrera 4", "Belarus");
        assertEquals(new DoubleValue(130030), result);
        result = TestUtils.invoke(instance, "CarPrice3", "", "", "", "");
        assertNull(result);
    }

    @Test
    public void greeting102() {
        Object result = TestUtils.invoke(instance, "Greeting102", 0);
        assertEquals("Good Morning, World! ruleNumber:0", result);
        result = TestUtils.invoke(instance, "Greeting102", 11);
        assertEquals("Good Morning, World! ruleNumber:0", result);
        result = TestUtils.invoke(instance, "Greeting102", 12);
        assertEquals("Good Afternoon, World! ruleNumber:1", result);
        result = TestUtils.invoke(instance, "Greeting102", 15);
        assertEquals("Good Afternoon, World! ruleNumber:1", result);
        result = TestUtils.invoke(instance, "Greeting102", 17);
        assertEquals("Good Afternoon, World! ruleNumber:1", result);
        result = TestUtils.invoke(instance, "Greeting102", 18);
        assertEquals("Good Evening, World! ruleNumber:2", result);
        result = TestUtils.invoke(instance, "Greeting102", 20);
        assertEquals("Good Evening, World! ruleNumber:2", result);
        result = TestUtils.invoke(instance, "Greeting102", 21);
        assertEquals("Good Evening, World! ruleNumber:2", result);
        result = TestUtils.invoke(instance, "Greeting102", 22);
        assertEquals("Good Night, World! ruleNumber:3", result);
        result = TestUtils.invoke(instance, "Greeting102", 23);
        assertEquals("Good Night, World! ruleNumber:3", result);
    }
}
