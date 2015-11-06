package org.openl.util.print;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openl.base.INamedThing;

public class FormatterTest {

    @Test
    public void testMap() {
        Map<Integer, String> testMap = new HashMap<Integer, String>();
        testMap.put(Integer.valueOf(25), "yo265");
        testMap.put(Integer.valueOf(1536), "abra");
        testMap.put(Integer.valueOf(4657), "cadabra");
        testMap.put(Integer.valueOf(985643), "matata");

        String busStr = printBusView(testMap);

        assertTrue(StringUtils.contains(busStr, "HashMap<Integer,String>"));
        assertTrue(StringUtils.contains(busStr, "985643 : matata"));
        assertTrue(StringUtils.contains(busStr, "4657 : cadabra"));
        assertTrue(StringUtils.contains(busStr, "25 : yo265"));
        assertTrue(StringUtils.contains(busStr, "1536 : abra"));

        String devStr = printDevView(testMap);
        assertTrue(StringUtils.contains(devStr, "HashMap<Integer,String>"));
        assertTrue(StringUtils.contains(devStr, "... 3 more}"));
    }

    private String printBusView(Object value) {
        StringBuilder strBuf = new StringBuilder();
        return Formatter.format(value, INamedThing.REGULAR, strBuf).toString();
    }

    private String printDevView(Object value) {
        StringBuilder strBuf = new StringBuilder();
        return Formatter.format(value, INamedThing.SHORT, strBuf).toString();
    }

    @Test
    public void testVector() {
        Vector<String> strVector = new Vector<String>();
        strVector.add("first");
        strVector.add("second");
        strVector.add("third");
        strVector.add("fourth");

        String busStr = printBusView(strVector);

        assertTrue(StringUtils.contains(busStr, "Vector<String>"));
        assertTrue(StringUtils.contains(busStr, "first"));
        assertTrue(StringUtils.contains(busStr, "second"));
        assertTrue(StringUtils.contains(busStr, "third"));
        assertTrue(StringUtils.contains(busStr, "fourth"));

        String devStr = printDevView(strVector);
        assertTrue(StringUtils.contains(devStr, "Vector<String>"));
        assertTrue(StringUtils.contains(devStr, "... 3 more"));
    }

    @Test
    public void testArray() {
        Integer[] intMas = new Integer[3];
        intMas[0] = Integer.valueOf(345);
        intMas[1] = Integer.valueOf(4567);
        intMas[2] = Integer.valueOf(76442);

        String busStr = printBusView(intMas);
        assertTrue(StringUtils.contains(busStr, "[345, 4567, 76442]"));

        String devStr = printDevView(intMas);
        assertTrue(StringUtils.contains(devStr, "[345, ... 2 more]"));
    }

    @Test
    public void testPrimritiveArray() {
        int[] intMas = new int[3];
        intMas[0] = 345;
        intMas[1] = 4567;
        intMas[2] = 76442;

        String busStr = printBusView(intMas);
        assertTrue(StringUtils.contains(busStr, "[345, 4567, 76442]"));

        String devStr = printDevView(intMas);
        assertTrue(StringUtils.contains(devStr, "[345, ... 2 more]"));
    }

    @Test
    public void testString() {
        String str = "text to format";

        String busStr = printBusView(str);
        assertEquals(str, busStr);

        String devStr = printDevView(str);
        assertEquals(str, devStr);
    }
}
