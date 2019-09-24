package org.openl.rules.helpers;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

public class DateRangeParsingTest {

    @Test
    public void testToString() {
        assertEquals("[02/11/2020 01:01:01; 02/11/2020 01:01:01]", new DateRange("2/11/2020 01:01:1").toString());
        assertEquals("[12/12/2019 00:00:00; 12/12/2019 00:00:00]", new DateRange("12/12/2019").toString());
        assertEquals("[03/12/2019 00:00:00; 03/12/2019 00:00:00]", new DateRange("03/12/2019").toString());

        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00]", new DateRange("03/12/2019 - 12/01/2019").toString());
        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00]", new DateRange("03/12/2019..12/01/2019").toString());
        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00)", new DateRange("03/12/2019 … 12/01/2019").toString());
        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00)",
            new DateRange("03/12/2019 ... 12/01/2019").toString());

        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00]",
            new DateRange("[03/12/2019; 12/01/2019]").toString());
        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00]", new DateRange("(03/12/2019;12/01/2019]").toString());
        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00)",
            new DateRange("[03/12/2019; 12/01/2019)").toString());
        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00)",
            new DateRange("(03/12/2019; 12/01/2019)").toString());

        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00)",
            new DateRange("(03/12/2019 .. 12/01/2019)").toString());
        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00]",
            new DateRange("[03/12/2019 .. 12/01/2019]").toString());
        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00]",
            new DateRange("(03/12/2019 .. 12/01/2019]").toString());
        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00)",
            new DateRange("[03/12/2019 .. 12/01/2019)").toString());

        assertEquals(">= 03/12/2019 00:00:00", new DateRange("03/12/2019 and more").toString());
        assertEquals("<= 03/12/2019 00:00:00", new DateRange("03/12/2019 or less").toString());

        assertEquals("> 03/12/2019 00:00:00", new DateRange("more than 03/12/2019").toString());
        assertEquals("< 12/01/2019 00:00:00", new DateRange("less than 12/01/2019").toString());

        assertEquals(">= 03/12/2019 00:00:00", new DateRange(">= 03/12/2019").toString());
        assertEquals("<= 03/12/2019 00:00:00", new DateRange("<= 03/12/2019").toString());

        assertEquals("> 03/12/2019 00:00:00", new DateRange("> 03/12/2019").toString());
        assertEquals("< 12/01/2019 00:00:00", new DateRange("< 12/01/2019").toString());
        assertEquals(">= 03/12/2019 00:00:00", new DateRange("03/12/2019+").toString());

        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00]",
            new DateRange(">=03/12/2019 <=12/01/2019").toString());
        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00]",
            new DateRange("<=12/01/2019 >=03/12/2019").toString());

        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00)",
            new DateRange(">=03/12/2019 <12/01/2019").toString());
        assertEquals("[03/12/2019 00:00:00; 12/01/2019 00:00:00)",
            new DateRange("<12/01/2019 >=03/12/2019").toString());

        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00]",
            new DateRange(">03/12/2019 <=12/01/2019").toString());
        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00]",
            new DateRange("<=12/01/2019 >03/12/2019").toString());

        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00)", new DateRange(">03/12/2019 <12/01/2019").toString());
        assertEquals("(03/12/2019 00:00:00; 12/01/2019 00:00:00)", new DateRange("<12/01/2019 >03/12/2019").toString());
    }

    @Test
    public void testEquals() throws ParseException {
        assertEquals(new DateRange(toDate("03/12/2019 00:00:00"), toDate("03/12/2019 00:00:00")),
            new DateRange("03/12/2019"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("03/12/2019-12/01/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("03/12/2019..12/01/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("03/12/2019 … 12/01/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("03/12/2019 ... 12/01/2019"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("[03/12/2019; 12/01/2019]"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("(03/12/2019;12/01/2019]"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("[03/12/2019; 12/01/2019)"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("(03/12/2019; 12/01/2019)"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("(03/12/2019 .. 12/01/2019)"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("[03/12/2019 .. 12/01/2019]"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("(03/12/2019 .. 12/01/2019]"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("[03/12/2019 .. 12/01/2019)"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                new Date(Long.MAX_VALUE),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("03/12/2019 and more"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                new Date(Long.MAX_VALUE),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("  03/12/2019   and   more  "));
        
        assertEquals(
            new DateRange(new Date(Long.MIN_VALUE),
                toDate("03/12/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("03/12/2019 or less"));
        assertEquals(
            new DateRange(new Date(Long.MIN_VALUE),
                toDate("03/12/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("  03/12/2019   or   less  "));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                new Date(Long.MAX_VALUE),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("more than 03/12/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                new Date(Long.MAX_VALUE),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("  more   than   03/12/2019  "));

        assertEquals(
            new DateRange(new Date(Long.MIN_VALUE),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("less than 12/01/2019"));
        assertEquals(
            new DateRange(new Date(Long.MIN_VALUE),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("  less   than   12/01/2019  "));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                new Date(Long.MAX_VALUE),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange(">= 03/12/2019"));
        assertEquals(
            new DateRange(new Date(Long.MIN_VALUE),
                toDate("03/12/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("<= 03/12/2019"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                new Date(Long.MAX_VALUE),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("> 03/12/2019"));
        assertEquals(
            new DateRange(new Date(Long.MIN_VALUE),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("< 12/01/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                new Date(Long.MAX_VALUE),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("03/12/2019+"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange(">=03/12/2019 <=12/01/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("<=12/01/2019 >=03/12/2019"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange(">=03/12/2019 <12/01/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.INCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("<12/01/2019 >=03/12/2019"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange(">03/12/2019 <=12/01/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.INCLUDING),
            new DateRange("<=12/01/2019 >03/12/2019"));

        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange(">03/12/2019 <12/01/2019"));
        assertEquals(
            new DateRange(toDate("03/12/2019 00:00:00"),
                toDate("12/01/2019 00:00:00"),
                ARangeParser.ParseStruct.BoundType.EXCLUDING,
                ARangeParser.ParseStruct.BoundType.EXCLUDING),
            new DateRange("<12/01/2019 >03/12/2019"));
    }

    @Test
    public void testSimpleRangeFormat() throws ParseException {
        DateRange range = new DateRange("03/12/2019");
        assertInclude(range, "03/12/2019 00:00:00");
        assertExclude(range, "03/12/2019 00:00:01", "03/11/2019 23:59:59");

        range = new DateRange("03/12/2019 01:01:22");
        assertInclude(range, "03/12/2019 01:01:22");
        assertExclude(range, "03/12/2019 01:01:23", "03/11/2019 01:01:21");

        range = new DateRange("3/2/2019 1:1:2");
        assertInclude(range, "03/02/2019 01:01:02");
        assertExclude(range, "03/02/2019 01:01:03", "03/11/2019 01:01:01");
    }

    @Test
    public void testMinMaxRangeFormat() throws ParseException {
        DateRange range = new DateRange("03/12/2019 - 12/01/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange("03/12/2019 .. 12/01/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange("03/12/2019 ... 12/01/2019");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
            "03/11/2019 23:59:59",
            "12/01/2019 00:00:01",
            "03/12/2019 00:00:00",
            "12/01/2019 00:00:00");

        range = new DateRange("03/12/2019 ... 12/01/2019");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
            "03/11/2019 23:59:59",
            "12/01/2019 00:00:01",
            "03/12/2019 00:00:00",
            "12/01/2019 00:00:00");
    }

    @Test
    public void testVerbal() throws ParseException {
        DateRange range = new DateRange("03/12/2019 and more");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00",
            "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59");

        range = new DateRange("12/01/2019 or less");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00",
            "03/11/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:01");

        range = new DateRange("more than 03/12/2019");
        assertInclude(range,
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00",
            "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59", "03/12/2019 00:00:00");

        range = new DateRange("less than 12/01/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "03/11/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:01", "12/01/2019 00:00:00");
    }

    @Test
    public void testMoreLessFormat() throws ParseException {
        DateRange range = new DateRange(">= 03/12/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00",
            "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59");

        range = new DateRange("<= 12/01/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00",
            "03/11/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:01");

        range = new DateRange("> 03/12/2019");
        assertInclude(range,
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00",
            "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59", "03/12/2019 00:00:00");

        range = new DateRange("< 12/01/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "03/11/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:01", "12/01/2019 00:00:00");

        range = new DateRange("03/12/2019 +");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00",
            "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59");
    }

    @Test
    public void testBracketsFormat() throws ParseException {
        DateRange range = new DateRange("[03/12/2019; 12/01/2019]");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange("[03/12/2019; 12/01/2019)");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "12/01/2019 00:00:00");

        range = new DateRange("(03/12/2019; 12/01/2019]");
        assertInclude(range,
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "03/12/2019 00:00:00");

        range = new DateRange("(03/12/2019; 12/01/2019)");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
            "03/11/2019 23:59:59",
            "12/01/2019 00:00:01",
            "03/12/2019 00:00:00",
            "12/01/2019 00:00:00");
    }

    @Test
    public void testMoreLessFormatBothBounds() throws ParseException {
        DateRange range = new DateRange(">=03/12/2019 <=12/01/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange("<=12/01/2019 >=03/12/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange(">=03/12/2019 <12/01/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "12/01/2019 00:00:00");

        range = new DateRange("<12/01/2019 >=03/12/2019");
        assertInclude(range,
            "03/12/2019 00:00:00",
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "12/01/2019 00:00:00");

        range = new DateRange(">03/12/2019 <=12/01/2019");
        assertInclude(range,
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "03/12/2019 00:00:00");

        range = new DateRange("<=12/01/2019 >03/12/2019");
        assertInclude(range,
            "03/12/2019 00:00:01",
            "08/01/2019 23:00:00",
            "11/30/2019 23:59:59",
            "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "03/12/2019 00:00:00");

        range = new DateRange(">03/12/2019 <12/01/2019");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
            "03/11/2019 23:59:59",
            "12/01/2019 00:00:01",
            "03/12/2019 00:00:00",
            "12/01/2019 00:00:00");

        range = new DateRange("<12/01/2019 >03/12/2019");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
            "03/11/2019 23:59:59",
            "12/01/2019 00:00:01",
            "03/12/2019 00:00:00",
            "12/01/2019 00:00:00");
    }

    @Test
    public void testNulls() {
        DateRange range = new DateRange("<12/01/2019 >03/12/2019");
        assertFalse(range.contains(null));
    }

    @Test
    public void testNegativeCases() {
        assertParseException(null);
        assertParseException("foo");
        assertParseException("3/2/2019 1:1:");
        assertParseException("3/2/2019 1:1");
        assertParseException("3/2/2019 1:");
        assertParseException("3/2/2019 1");
        assertParseException("3/2/");
        assertParseException("3/2");
        assertParseException("3/");
        assertParseException("3");
        assertParseException("");
        assertParseException("3/2/2019 1:1:1 sdsdsd");
        assertParseException("sdsdsd 3/2/2019 1:1:1");

        try {
            new DateRange("2/11/2020 99:99:99");
            fail("Must be failed!");
        } catch (RuntimeException e) {
            assertEquals("Failed to parse a range: 2/11/2020 99:99:99", e.getMessage());
        }

        try {
            new DateRange("2/99/2020 12:1:1");
            fail("Must be failed!");
        } catch (RuntimeException e) {
            assertEquals("Failed to parse a range: 2/99/2020 12:1:1", e.getMessage());
        }
    }

    private void assertParseException(String range) {
        try {
            new DateRange(range);
            fail("Must be failed!");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().startsWith("Failed to parse a range: "));
        }
    }

    private void assertInclude(DateRange range, String... args) throws ParseException {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertTrue(String.format("The range %s must include a date \"%s\"", range.toString(), s),
                range.contains(toDate(s)));
        }
    }

    private void assertExclude(DateRange range, String... args) throws ParseException {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertFalse(String.format("The range %s must not include a date \"%s\"", range.toString(), s),
                range.contains(toDate(s)));
        }
    }

    private Date toDate(String s) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
        formatter.setLenient(false); // Strict matching
        formatter.getCalendar().set(0, 0, 0, 0, 0, 0); // at
        formatter.getCalendar().set(Calendar.MILLISECOND, 0);
        return formatter.parse(s);
    }

}
