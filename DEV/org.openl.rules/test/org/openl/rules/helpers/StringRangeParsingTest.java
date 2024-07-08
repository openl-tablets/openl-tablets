package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.range.Range;

public class StringRangeParsingTest {

    @Test
    public void testToString() {
        assertEquals("B", new StringRange("B").toString());

        assertEquals("[AA..ZZ]", new StringRange("AA-ZZ").toString());
        assertEquals("[AA..ZZ]", new StringRange("AA..ZZ").toString());
        assertEquals("(AA..ZZ)", new StringRange("AA … ZZ").toString());
        assertEquals("(AA..ZZ)", new StringRange("AA ... ZZ").toString());

        assertEquals("[AA..ZZ]", new StringRange("[AA; ZZ]").toString());
        assertEquals("(AA..ZZ]", new StringRange("(AA;ZZ]").toString());
        assertEquals("[AA..ZZ)", new StringRange("[AA; ZZ)").toString());
        assertEquals("(AA..ZZ)", new StringRange("(AA; ZZ)").toString());

        assertEquals("(AA..ZZ)", new StringRange("(AA .. ZZ)").toString());
        assertEquals("[AA..ZZ]", new StringRange("[AA .. ZZ]").toString());
        assertEquals("(AA..ZZ]", new StringRange("(AA .. ZZ]").toString());
        assertEquals("[AA..ZZ)", new StringRange("[AA .. ZZ)").toString());

        assertEquals(">= AA", new StringRange("AA and more").toString());
        assertEquals("<= AA", new StringRange("AA or less").toString());

        assertEquals("> AA", new StringRange("more than AA").toString());
        assertEquals("< ZZ", new StringRange("less than ZZ").toString());

        assertEquals(">= AA", new StringRange(">= AA").toString());
        assertEquals("<= AA", new StringRange("<= AA").toString());

        assertEquals("> AA", new StringRange("> AA").toString());
        assertEquals("< ZZ", new StringRange("< ZZ").toString());
        assertEquals(">= AA", new StringRange("AA+").toString());

        assertEquals("[AA..ZZ]", new StringRange(">=AA <=ZZ").toString());
        assertEquals("[AA..ZZ]", new StringRange("<=ZZ >=AA").toString());

        assertEquals("[AA..ZZ)", new StringRange(">=AA <ZZ").toString());
        assertEquals("[AA..ZZ)", new StringRange("<ZZ >=AA").toString());

        assertEquals("(AA..ZZ]", new StringRange(">AA <=ZZ").toString());
        assertEquals("(AA..ZZ]", new StringRange("<=ZZ >AA").toString());

        assertEquals("(AA..ZZ)", new StringRange(">AA <ZZ").toString());
        assertEquals("(AA..ZZ)", new StringRange("<ZZ >AA").toString());
    }

    @Test
    public void testToStringWhitespaces() {
        // Part 1
        assertEquals("[A  A..Z  Z]", new StringRange("A  A-Z  Z").toString());
        assertEquals("[A  A..Z  Z]", new StringRange("A  A..Z  Z").toString());
        assertEquals("(A  A..Z  Z)", new StringRange("A  A … Z  Z").toString());
        assertEquals("(A  A..Z  Z)", new StringRange("A  A ... Z  Z").toString());

        assertEquals("[A  A..Z  Z]", new StringRange("[A  A; Z  Z]").toString());
        assertEquals("(A  A..Z  Z]", new StringRange("(A  A;Z  Z]").toString());
        assertEquals("[A  A..Z  Z)", new StringRange("[A  A; Z  Z)").toString());
        assertEquals("(A  A..Z  Z)", new StringRange("(A  A; Z  Z)").toString());

        assertEquals("(A  A..Z  Z)", new StringRange("(A  A .. Z  Z)").toString());
        assertEquals("[A  A..Z  Z]", new StringRange("[A  A .. Z  Z]").toString());
        assertEquals("(A  A..Z  Z]", new StringRange("(A  A .. Z  Z]").toString());
        assertEquals("[A  A..Z  Z)", new StringRange("[A  A .. Z  Z)").toString());

        assertEquals(">= A  A", new StringRange("A  A and more").toString());
        assertEquals("<= A  A", new StringRange("A  A or less").toString());

        assertEquals("> A  A", new StringRange("more than A  A").toString());
        assertEquals("< Z  Z", new StringRange("less than Z  Z").toString());

        assertEquals(">= A  A", new StringRange(">= A  A").toString());
        assertEquals("<= A  A", new StringRange("<= A  A").toString());

        assertEquals("> A  A", new StringRange("> A  A").toString());
        assertEquals("< Z  Z", new StringRange("< Z  Z").toString());
        assertEquals(">= A  A", new StringRange("A  A+").toString());

        assertEquals("[A  A..Z  Z]", new StringRange(">=A  A <=Z  Z").toString());
        assertEquals("[A  A..Z  Z]", new StringRange("<=Z  Z >=A  A").toString());

        assertEquals("[A  A..Z  Z)", new StringRange(">=A  A <Z  Z").toString());
        assertEquals("[A  A..Z  Z)", new StringRange("<Z  Z >=A  A").toString());

        assertEquals("(A  A..Z  Z]", new StringRange(">A  A <=Z  Z").toString());
        assertEquals("(A  A..Z  Z]", new StringRange("<=Z  Z >A  A").toString());

        assertEquals("(A  A..Z  Z)", new StringRange(">A  A <Z  Z").toString());
        assertEquals("(A  A..Z  Z)", new StringRange("<Z  Z >A  A").toString());

        // Part 2
        assertEquals("B", new StringRange("  B  ").toString());
        assertEquals("[AA..ZZ]", new StringRange("  AA  -  ZZ  ").toString());
        assertEquals("[AA..ZZ]", new StringRange("  AA  ..  ZZ  ").toString());
        assertEquals("(AA..ZZ)", new StringRange("  AA   …   ZZ  ").toString());
        assertEquals("(AA..ZZ)", new StringRange("  AA   ...   ZZ  ").toString());

        assertEquals("[AA..ZZ]", new StringRange("  [AA  ;   ZZ  ]  ").toString());
        assertEquals("(AA..ZZ]", new StringRange("  (AA  ;   ZZ  ]  ").toString());
        assertEquals("[AA..ZZ)", new StringRange("  [AA  ;   ZZ  )  ").toString());
        assertEquals("(AA..ZZ)", new StringRange("  (AA  ;   ZZ  )  ").toString());

        assertEquals("(AA..ZZ)", new StringRange("  (  AA   ..   ZZ  )  ").toString());
        assertEquals("[AA..ZZ]", new StringRange("  [  AA   ..   ZZ  ]  ").toString());
        assertEquals("(AA..ZZ]", new StringRange("  (  AA   ..   ZZ  ]  ").toString());
        assertEquals("[AA..ZZ)", new StringRange("  [  AA   ..   ZZ  )  ").toString());

        assertEquals(">= AA", new StringRange("  AA   and   more  ").toString());
        assertEquals("<= AA", new StringRange("  AA   or   less  ").toString());

        assertEquals("> AA", new StringRange("  more   than   AA  ").toString());
        assertEquals("< ZZ", new StringRange("  less   than   ZZ  ").toString());

        assertEquals(">= AA", new StringRange("  >=   AA  ").toString());
        assertEquals("<= AA", new StringRange("  <=   AA  ").toString());

        assertEquals("> AA", new StringRange("  >   AA  ").toString());
        assertEquals("< ZZ", new StringRange("  <   ZZ  ").toString());
        assertEquals(">= AA", new StringRange("  AA+  ").toString());

        assertEquals("[AA..ZZ]", new StringRange("  >=  AA   <=  ZZ  ").toString());
        assertEquals("[AA..ZZ]", new StringRange("  <=  ZZ   >=  AA  ").toString());

        assertEquals("[AA..ZZ)", new StringRange("  >=  AA   <  ZZ  ").toString());
        assertEquals("[AA..ZZ)", new StringRange("  <  ZZ   >=  AA  ").toString());

        assertEquals("(AA..ZZ]", new StringRange("  >  AA   <=  ZZ  ").toString());
        assertEquals("(AA..ZZ]", new StringRange("  <=  ZZ   >  AA  ").toString());

        assertEquals("(AA..ZZ)", new StringRange("  >  AA   <  ZZ  ").toString());
        assertEquals("(AA..ZZ)", new StringRange("  <  ZZ   >  AA  ").toString());

        //Part 3
        assertEquals("[A  A..Z  Z]", new StringRange("  A  A  -  Z  Z  ").toString());
        assertEquals("[A  A..Z  Z]", new StringRange("  A  A  ..  Z  Z  ").toString());
        assertEquals("(A  A..Z  Z)", new StringRange("  A  A   …   Z  Z  ").toString());
        assertEquals("(A  A..Z  Z)", new StringRange("  A  A   ...   Z  Z  ").toString());

        assertEquals("[A  A..Z  Z]", new StringRange("  [A  A  ;   Z  Z  ]  ").toString());
        assertEquals("(A  A..Z  Z]", new StringRange("  (A  A  ;   Z  Z  ]  ").toString());
        assertEquals("[A  A..Z  Z)", new StringRange("  [A  A  ;   Z  Z  )  ").toString());
        assertEquals("(A  A..Z  Z)", new StringRange("  (A  A  ;   Z  Z  )  ").toString());

        assertEquals("(A  A..Z  Z)", new StringRange("  (  A  A   ..   Z  Z  )  ").toString());
        assertEquals("[A  A..Z  Z]", new StringRange("  [  A  A   ..   Z  Z  ]  ").toString());
        assertEquals("(A  A..Z  Z]", new StringRange("  (  A  A   ..   Z  Z  ]  ").toString());
        assertEquals("[A  A..Z  Z)", new StringRange("  [  A  A   ..   Z  Z  )  ").toString());

        assertEquals(">= A  A", new StringRange("  A  A   and   more  ").toString());
        assertEquals("<= A  A", new StringRange("  A  A   or   less  ").toString());

        assertEquals("> A  A", new StringRange("  more   than   A  A  ").toString());
        assertEquals("< Z  Z", new StringRange("  less   than   Z  Z  ").toString());

        assertEquals(">= A  A", new StringRange("  >=   A  A  ").toString());
        assertEquals("<= A  A", new StringRange("  <=   A  A  ").toString());

        assertEquals("> A  A", new StringRange("  >   A  A  ").toString());
        assertEquals("< Z  Z", new StringRange("  <   Z  Z  ").toString());
        assertEquals(">= A  A", new StringRange("  A  A+  ").toString());

        assertEquals("[A  A..Z  Z]", new StringRange("  >=  A  A   <=  Z  Z  ").toString());
        assertEquals("[A  A..Z  Z]", new StringRange("  <=  Z  Z   >=  A  A  ").toString());

        assertEquals("[A  A..Z  Z)", new StringRange("  >=  A  A   <  Z  Z  ").toString());
        assertEquals("[A  A..Z  Z)", new StringRange("  <  Z  Z   >=  A  A  ").toString());

        assertEquals("(A  A..Z  Z]", new StringRange("  >  A  A   <=  Z  Z  ").toString());
        assertEquals("(A  A..Z  Z]", new StringRange("  <=  Z  Z   >  A  A  ").toString());

        assertEquals("(A  A..Z  Z)", new StringRange("  >  A  A   <  Z  Z  ").toString());
        assertEquals("(A  A..Z  Z)", new StringRange("  <  Z  Z   >  A  A  ").toString());

    }

    @Test
    public void testDoubleDash() {
        assertEquals("[Mister-X..Ray]", new StringRange("Mister-X - Ray").toString());
        assertEquals("[Mister..X - Ray]", new StringRange("Mister - X - Ray").toString());
        assertEquals("[Mister..X-Ray]", new StringRange("Mister - X-Ray").toString());
        assertEquals("[Mister..X-Ray]", new StringRange("Mister-X-Ray").toString());

        assertEquals("(Mister-X..Ray)", new StringRange("(Mister-X - Ray)").toString());
        assertEquals("(Mister..X - Ray)", new StringRange("(Mister - X - Ray)").toString());
        assertEquals("(Mister..X-Ray)", new StringRange("(Mister - X-Ray)").toString());
        assertEquals("(Mister..X-Ray)", new StringRange("(Mister-X-Ray)").toString());

        assertEquals("[Mister-..Ray]", new StringRange("Mister- - Ray").toString());
        assertEquals("[Mister..Ray -]", new StringRange("Mister - Ray - ").toString());
        assertEquals("[-Mister..-Ray]", new StringRange("-Mister - -Ray").toString());
        assertEquals("[-Mister..-Ray]", new StringRange("-Mister--Ray").toString());
        assertEquals("[- Mister..- Ray]", new StringRange("- Mister - - Ray").toString());
        assertEquals("[- Mister..- Ray - - X]", new StringRange(" - Mister - - Ray - - X ").toString());


        assertEquals("(Mister-..Ray)", new StringRange("(Mister- - Ray)").toString());
        assertEquals("(Mister..Ray -)", new StringRange("(Mister - Ray - )").toString());
        assertEquals("(-Mister..-Ray)", new StringRange("(-Mister - -Ray)").toString());
        assertEquals("(-Mister..-Ray)", new StringRange("(-Mister--Ray)").toString());
        assertEquals("(- Mister..- Ray)", new StringRange("(- Mister - - Ray)").toString());
        assertEquals("(- Mister..- Ray - - X)", new StringRange("( - Mister - - Ray - - X )").toString());
    }

    @Test
    public void testSpecialCases() {
        assertEquals("[.aaa....bbb.]", new StringRange(".aaa.-.bbb.").toString());
        assertEquals("[\\aaa\\..\\bbb\\]", new StringRange("\\aaa\\-\\bbb\\").toString());
        assertEquals("[a not so long string with the spaces in the middle and with-the-dashes-in-the-long-words" +
                        " becomes truncated when it defines in the String range..should work correctly]",
                new StringRange("a not so long string with the spaces in the middle and with-the-dashes-in-the-long-words" +
                        " becomes truncated when it defines in the String range - should work correctly").toString());

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
        assertFalse(range.contains((Range<CharSequence>) null));
        assertFalse(range.contains((CharSequence) null));
    }

    @Test
    public void testNegative() {
        StringRange range = new StringRange("00F-00Z");
        assertFalse(range.contains("Z"));
    }

    @Test
    public void testParseException() {
        assertThrows(RuntimeException.class, () -> {
            new StringRange(null);
        });
    }

    private void assertInclude(StringRange range, String... args) {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertTrue(range.contains(s),
                    String.format("The range %s must include a string '%s'", range.toString(), s));
        }
    }

    private void assertExclude(StringRange range, String... args) {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertFalse(range.contains(s),
                    String.format("The range %s must not include a string '%s'", range.toString(), s));
        }
    }

}
