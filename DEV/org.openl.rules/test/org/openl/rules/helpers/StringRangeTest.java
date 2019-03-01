package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;

public class StringRangeTest {

    @Test
    public void testToString() {
        assertEquals("[B; B]", new StringRange("B").toString());

        assertEquals("[AA; ZZ]", new StringRange("AA-ZZ").toString());
        assertEquals("[AA; ZZ]", new StringRange("AA..ZZ").toString());
        assertEquals("(AA; ZZ)", new StringRange("AA … ZZ").toString());
        assertEquals("(AA; ZZ)", new StringRange("AA ... ZZ").toString());

        assertEquals("[AA; ZZ]", new StringRange("[AA; ZZ]").toString());
        assertEquals("(AA; ZZ]", new StringRange("(AA;ZZ]").toString());
        assertEquals("[AA; ZZ)", new StringRange("[AA; ZZ)").toString());
        assertEquals("(AA; ZZ)", new StringRange("(AA; ZZ)").toString());

        assertEquals("(AA; ZZ)", new StringRange("(AA .. ZZ)").toString());
        assertEquals("[AA; ZZ]", new StringRange("[AA .. ZZ]").toString());
        assertEquals("(AA; ZZ]", new StringRange("(AA .. ZZ]").toString());
        assertEquals("[AA; ZZ)", new StringRange("[AA .. ZZ)").toString());

        assertEquals(">= AA", new StringRange("AA and more").toString());
        assertEquals("<= AA", new StringRange("AA or less").toString());

        assertEquals("> AA", new StringRange("more than AA").toString());
        assertEquals("< ZZ", new StringRange("less than ZZ").toString());

        assertEquals(">= AA", new StringRange(">= AA").toString());
        assertEquals("<= AA", new StringRange("<= AA").toString());

        assertEquals("> AA", new StringRange("> AA").toString());
        assertEquals("< ZZ", new StringRange("< ZZ").toString());
        assertEquals(">= AA", new StringRange("AA+").toString());

        assertEquals("[AA; ZZ]", new StringRange(">=AA <=ZZ").toString());
        assertEquals("[AA; ZZ]", new StringRange("<=ZZ >=AA").toString());

        assertEquals("[AA; ZZ)", new StringRange(">=AA <ZZ").toString());
        assertEquals("[AA; ZZ)", new StringRange("<ZZ >=AA").toString());

        assertEquals("(AA; ZZ]", new StringRange(">AA <=ZZ").toString());
        assertEquals("(AA; ZZ]", new StringRange("<=ZZ >AA").toString());

        assertEquals("(AA; ZZ)", new StringRange(">AA <ZZ").toString());
        assertEquals("(AA; ZZ)", new StringRange("<ZZ >AA").toString());
    }

    @Test
    public void testEquals() {
        assertEquals(new StringRange("B", "B"), new StringRange("B"));

        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("AA-ZZ"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("AA..ZZ"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.EXCLUDING), new StringRange("AA … ZZ"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.EXCLUDING), new StringRange("AA ... ZZ"));

        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("[AA; ZZ]"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.INCLUDING), new StringRange("(AA;ZZ]"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.EXCLUDING), new StringRange("[AA; ZZ)"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.EXCLUDING), new StringRange("(AA; ZZ)"));

        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.EXCLUDING), new StringRange("(AA .. ZZ)"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("[AA .. ZZ]"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.INCLUDING), new StringRange("(AA .. ZZ]"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.EXCLUDING), new StringRange("[AA .. ZZ)"));

        assertEquals(new StringRange("AA", null, BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("AA and more"));
        assertEquals(new StringRange(null, "AA", BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("AA or less"));

        assertEquals(new StringRange("AA", null, BoundType.EXCLUDING, BoundType.INCLUDING), new StringRange("more than AA"));
        assertEquals(new StringRange(null, "ZZ", BoundType.INCLUDING, BoundType.EXCLUDING), new StringRange("less than ZZ"));

        assertEquals(new StringRange("AA", null, BoundType.INCLUDING, BoundType.INCLUDING), new StringRange(">= AA"));
        assertEquals(new StringRange(null, "AA", BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("<= AA"));

        assertEquals(new StringRange("AA", null, BoundType.EXCLUDING, BoundType.INCLUDING), new StringRange("> AA"));
        assertEquals(new StringRange(null, "ZZ", BoundType.INCLUDING, BoundType.EXCLUDING), new StringRange("< ZZ"));
        assertEquals(new StringRange("AA", null, BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("AA+"));

        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.INCLUDING), new StringRange(">=AA <=ZZ"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.INCLUDING), new StringRange("<=ZZ >=AA"));

        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.EXCLUDING), new StringRange(">=AA <ZZ"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.INCLUDING, BoundType.EXCLUDING), new StringRange("<ZZ >=AA"));

        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.INCLUDING), new StringRange(">AA <=ZZ"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.INCLUDING), new StringRange("<=ZZ >AA"));

        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.EXCLUDING), new StringRange(">AA <ZZ"));
        assertEquals(new StringRange("AA", "ZZ", BoundType.EXCLUDING, BoundType.EXCLUDING), new StringRange("<ZZ >AA"));
    }

    @Test
    public void testSimpleRangeFormat() {
        StringRange range = new StringRange("B");
        assertInclude(range, "B");
        assertExclude(range, "b", "A", "C");

        range = new StringRange("BBB");
        assertInclude(range, "BBB");
        assertExclude(range, "BB", "BBBB", "BBC", "BBA");
    }

    @Test
    public void testMinMaxRangeFormat() {
        StringRange range = new StringRange("AA-ZZ");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "ZZ", "Z");
        assertExclude(range, "A", "ZZZ", "aa", "zz");

        range = new StringRange("AA .. ZZ");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "ZZ", "Z");
        assertExclude(range, "A", "ZZZ", "aa", "zz");

        range = new StringRange("AA ... ZZ");
        assertInclude(range, "AAa", "B", "BBBBBB", "Z");
        assertExclude(range, "A", "AA", "ZZZ", "aa", "zz");

        range = new StringRange("AA … ZZ");
        assertInclude(range, "AAa", "B", "BBBBBB", "Z");
        assertExclude(range, "A", "AA", "ZZZ", "aa", "zz");
    }

    @Test
    public void testBracketsFormat() {
        StringRange range = new StringRange("[AA; ZZ]");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "ZZ", "Z");
        assertExclude(range, "A", "ZZZ", "aa", "zz");

        range = new StringRange("[AA; ZZ)");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "Z");
        assertExclude(range, "A", "ZZZ", "aa", "zz", "ZZ");

        range = new StringRange("(AA; ZZ]");
        assertInclude(range, "AAa", "B", "BBBBBB", "ZZ", "Z");
        assertExclude(range, "AA", "A", "ZZZ", "aa", "zz");

        range = new StringRange("(AA; ZZ)");
        assertInclude(range, "AAa", "B", "BBBBBB", "Z");
        assertExclude(range, "AA", "A", "ZZZ", "aa", "zz", "ZZ");
    }

    @Test
    public void testVerbal() {
        StringRange range = new StringRange("AA and more");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "ZZ", "Z", "ZZZ", "aa", "zz");
        assertExclude(range, "A");

        range = new StringRange("AA or less");
        assertInclude(range, "AA", "A");
        assertExclude(range, "AAa", "B", "BBBBBB", "ZZ", "Z", "ZZZ", "aa", "zz");

        range = new StringRange("more than AA");
        assertInclude(range, "AAa", "B", "BBBBBB", "ZZ", "Z", "ZZZ", "aa", "zz");
        assertExclude(range, "AA", "A");

        range = new StringRange("less than ZZ");
        assertInclude(range, "AAa", "B", "BBBBBB", "Z", "AA", "A");
        assertExclude(range, "ZZZ", "aa", "ZZ", "zz");
    }

    @Test
    public void testMoreLessFormat() {
        StringRange range = new StringRange(">= AA");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "ZZ", "Z", "ZZZ", "aa", "zz");
        assertExclude(range, "A");

        range = new StringRange("<= AA");
        assertInclude(range, "AA", "A");
        assertExclude(range, "AAa", "B", "BBBBBB", "ZZ", "Z", "ZZZ", "aa", "zz");

        range = new StringRange("> AA");
        assertInclude(range, "AAa", "B", "BBBBBB", "ZZ", "Z", "ZZZ", "aa", "zz");
        assertExclude(range, "AA", "A");

        range = new StringRange("< ZZ");
        assertInclude(range, "AAa", "B", "BBBBBB", "Z", "AA", "A");
        assertExclude(range, "ZZZ", "aa", "ZZ", "zz");

        range = new StringRange("AA+");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "ZZ", "Z", "ZZZ", "aa", "zz");
        assertExclude(range, "A");
    }

    @Test
    public void testMoreLessFormatBothBounds() {
        StringRange range = new StringRange(">=AA <=ZZ");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "ZZ", "Z");
        assertExclude(range, "A", "ZZZ", "aa", "zz");

        range = new StringRange("<=ZZ >=AA");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "ZZ", "Z");
        assertExclude(range, "A", "ZZZ", "aa", "zz");

        range = new StringRange(">=AA <ZZ");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "Z");
        assertExclude(range, "A", "ZZZ", "aa", "zz", "ZZ");

        range = new StringRange("<ZZ >=AA");
        assertInclude(range, "AA", "AAa", "B", "BBBBBB", "Z");
        assertExclude(range, "A", "ZZZ", "aa", "zz", "ZZ");

        range = new StringRange(">AA <=ZZ");
        assertInclude(range, "AAa", "B", "BBBBBB", "ZZ", "Z");
        assertExclude(range, "AA", "A", "ZZZ", "aa", "zz");

        range = new StringRange("<=ZZ >AA");
        assertInclude(range, "AAa", "B", "BBBBBB", "ZZ", "Z");
        assertExclude(range, "AA", "A", "ZZZ", "aa", "zz");

        range = new StringRange(">AA <ZZ");
        assertInclude(range, "AAa", "B", "BBBBBB", "Z");
        assertExclude(range, "AA", "A", "ZZZ", "aa", "zz", "ZZ");

        range = new StringRange("<ZZ >AA");
        assertInclude(range, "AAa", "B", "BBBBBB", "Z");
        assertExclude(range, "AA", "A", "ZZZ", "aa", "zz", "ZZ");
    }

    @Test
    public void testNulls() {
        StringRange range = new StringRange(">=AA <=ZZ");
        assertFalse(range.contains(null));
    }

    private void assertInclude(StringRange range, String... args) {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertTrue(String.format("The range %s must include a string \"%s\"", range.toString(), s),
                range.contains(s));
        }
    }

    private void assertExclude(StringRange range, String... args) {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertFalse(String.format("The range %s must not include a string \"%s\"", range.toString(), s),
                range.contains(s));
        }
    }

}
