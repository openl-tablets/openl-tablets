package org.openl.util.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import lombok.Getter;
import org.junit.jupiter.api.Test;

class FormatterTest {

    @Test
    void testMap() {
        var testMap = new HashMap<Integer, String>();
        testMap.put(25, "yo265");
        testMap.put(1536, "abra");
        testMap.put(4657, "cadabra");
        testMap.put(985643, "matata");

        var busStr = printBusView(testMap);

        assertContains(busStr, "HashMap<Integer,String>");
        assertContains(busStr, "985643 : matata");
        assertContains(busStr, "4657 : cadabra");
        assertContains(busStr, "25 : yo265");
        assertContains(busStr, "1536 : abra");

        testMap.put(983, "acuna");
        var devStr = printDevView(testMap);
        assertContains(devStr, "HashMap<Integer,String>");
        assertContains(devStr, "... 2 more}");
    }

    private String printBusView(Object value) {
        var strBuf = new StringBuilder();
        return DefaultFormat.format(value, strBuf).toString();
    }

    private String printDevView(Object value) {
        var strBuf = new StringBuilder();
        return DefaultFormat.format(value, strBuf).toString();
    }

    @Test
    void testVector() {
        var strVector = new Vector<String>();
        strVector.add("first");
        strVector.add("second");
        strVector.add("third");
        strVector.add("fourth");

        var busStr = printBusView(strVector);

        assertContains(busStr, "Vector<String>");
        assertContains(busStr, "first");
        assertContains(busStr, "second");
        assertContains(busStr, "third");
        assertContains(busStr, "fourth");

        strVector.add("fifth");
        var devStr = printDevView(strVector);
        assertContains(devStr, "Vector<String>");
        assertContains(devStr, "... 2 more");
    }

    @Test
    void testArray() {
        Integer[] intMas = new Integer[3];
        intMas[0] = 345;
        intMas[1] = 4567;
        intMas[2] = 76442;

        var busStr = printBusView(intMas);
        assertContains(busStr, "[345, 4567, 76442]");

        var devStr = printDevView(new Integer[]{1, 2, 3, 4, 5});
        assertContains(devStr, "[1, 2, 3, ... 2 more]");
    }

    @Test
    void testPrimritiveArray() {
        int[] intMas = new int[3];
        intMas[0] = 345;
        intMas[1] = 4567;
        intMas[2] = 76442;

        var busStr = printBusView(intMas);
        assertContains(busStr, "[345, 4567, 76442]");

        var devStr = printDevView(new int[]{1, 2, 3, 4, 5});
        assertContains(devStr, "[1, 2, 3, ... 2 more]");
    }

    @Test
    void testString() {
        var str = "text to format";

        var busStr = printBusView(str);
        assertEquals(str, busStr);

        var devStr = printDevView(str);
        assertEquals(str, devStr);
    }

    @Test
    void testBean() {
        var myType = new MyType("foo", 0.1, Arrays.asList("foo", "bar"), Locale.US);
        var busStr = printBusView(myType);
        assertEquals("FormatterTest$MyType(id=0){\n  d=0.1\n  list={\n    [0]=foo\n    [1]=bar\n    }\n  locale=en-US\n  str=foo\n  }",
                busStr);
    }

    private void assertContains(String text, String expected) {
        assertNotNull(text);
        assertTrue(text.contains(expected), text);
    }

    private static class MyType {

        @Getter
        private final String str;
        @Getter
        private final Double d;
        @Getter
        private final Locale locale;
        @Getter
        private final List<String> list;

        public MyType(String str, Double d, List<String> list, Locale locale) {
            this.str = str;
            this.d = d;
            this.list = list;
            this.locale = locale;
        }
    }
}
