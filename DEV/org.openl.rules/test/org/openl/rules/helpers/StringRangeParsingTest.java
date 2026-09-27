package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.range.Range;

class StringRangeParsingTest {

    static Stream<Arguments> testToString() {
        return Stream.of(
                arguments("B", "B"),

                arguments("AA-ZZ", "[AA..ZZ]"),
                arguments("AA..ZZ", "[AA..ZZ]"),
                arguments("AA … ZZ", "(AA..ZZ)"),
                arguments("AA ... ZZ", "(AA..ZZ)"),

                arguments("[AA; ZZ]", "[AA..ZZ]"),
                arguments("(AA;ZZ]", "(AA..ZZ]"),
                arguments("[AA; ZZ)", "[AA..ZZ)"),
                arguments("(AA; ZZ)", "(AA..ZZ)"),

                arguments("(AA .. ZZ)", "(AA..ZZ)"),
                arguments("[AA .. ZZ]", "[AA..ZZ]"),
                arguments("(AA .. ZZ]", "(AA..ZZ]"),
                arguments("[AA .. ZZ)", "[AA..ZZ)"),

                arguments("AA and more", ">= AA"),
                arguments("AA or less", "<= AA"),

                arguments("more than AA", "> AA"),
                arguments("less than ZZ", "< ZZ"),

                arguments(">= AA", ">= AA"),
                arguments("<= AA", "<= AA"),

                arguments("> AA", "> AA"),
                arguments("< ZZ", "< ZZ"),
                arguments("AA+", ">= AA"),

                arguments(">=AA <=ZZ", "[AA..ZZ]"),
                arguments("<=ZZ >=AA", "[AA..ZZ]"),

                arguments(">=AA <ZZ", "[AA..ZZ)"),
                arguments("<ZZ >=AA", "[AA..ZZ)"),

                arguments(">AA <=ZZ", "(AA..ZZ]"),
                arguments("<=ZZ >AA", "(AA..ZZ]"),

                arguments(">AA <ZZ", "(AA..ZZ)"),
                arguments("<ZZ >AA", "(AA..ZZ)"));
    }

    @ParameterizedTest
    @MethodSource
    void testToString(String range, String expected) {
        assertEquals(expected, new StringRange(range).toString());
    }

    static Stream<Arguments> testToStringWhitespaces() {
        return Stream.of(
                // Part 1
                arguments("A  A-Z  Z", "[A  A..Z  Z]"),
                arguments("A  A..Z  Z", "[A  A..Z  Z]"),
                arguments("A  A … Z  Z", "(A  A..Z  Z)"),
                arguments("A  A ... Z  Z", "(A  A..Z  Z)"),

                arguments("[A  A; Z  Z]", "[A  A..Z  Z]"),
                arguments("(A  A;Z  Z]", "(A  A..Z  Z]"),
                arguments("[A  A; Z  Z)", "[A  A..Z  Z)"),
                arguments("(A  A; Z  Z)", "(A  A..Z  Z)"),

                arguments("(A  A .. Z  Z)", "(A  A..Z  Z)"),
                arguments("[A  A .. Z  Z]", "[A  A..Z  Z]"),
                arguments("(A  A .. Z  Z]", "(A  A..Z  Z]"),
                arguments("[A  A .. Z  Z)", "[A  A..Z  Z)"),

                arguments("A  A and more", ">= A  A"),
                arguments("A  A or less", "<= A  A"),

                arguments("more than A  A", "> A  A"),
                arguments("less than Z  Z", "< Z  Z"),

                arguments(">= A  A", ">= A  A"),
                arguments("<= A  A", "<= A  A"),

                arguments("> A  A", "> A  A"),
                arguments("< Z  Z", "< Z  Z"),
                arguments("A  A+", ">= A  A"),

                arguments(">=A  A <=Z  Z", "[A  A..Z  Z]"),
                arguments("<=Z  Z >=A  A", "[A  A..Z  Z]"),

                arguments(">=A  A <Z  Z", "[A  A..Z  Z)"),
                arguments("<Z  Z >=A  A", "[A  A..Z  Z)"),

                arguments(">A  A <=Z  Z", "(A  A..Z  Z]"),
                arguments("<=Z  Z >A  A", "(A  A..Z  Z]"),

                arguments(">A  A <Z  Z", "(A  A..Z  Z)"),
                arguments("<Z  Z >A  A", "(A  A..Z  Z)"),

                // Part 2
                arguments("  B  ", "B"),
                arguments("  AA  -  ZZ  ", "[AA..ZZ]"),
                arguments("  AA  ..  ZZ  ", "[AA..ZZ]"),
                arguments("  AA   …   ZZ  ", "(AA..ZZ)"),
                arguments("  AA   ...   ZZ  ", "(AA..ZZ)"),

                arguments("  [AA  ;   ZZ  ]  ", "[AA..ZZ]"),
                arguments("  (AA  ;   ZZ  ]  ", "(AA..ZZ]"),
                arguments("  [AA  ;   ZZ  )  ", "[AA..ZZ)"),
                arguments("  (AA  ;   ZZ  )  ", "(AA..ZZ)"),

                arguments("  (  AA   ..   ZZ  )  ", "(AA..ZZ)"),
                arguments("  [  AA   ..   ZZ  ]  ", "[AA..ZZ]"),
                arguments("  (  AA   ..   ZZ  ]  ", "(AA..ZZ]"),
                arguments("  [  AA   ..   ZZ  )  ", "[AA..ZZ)"),

                arguments("  AA   and   more  ", ">= AA"),
                arguments("  AA   or   less  ", "<= AA"),

                arguments("  more   than   AA  ", "> AA"),
                arguments("  less   than   ZZ  ", "< ZZ"),

                arguments("  >=   AA  ", ">= AA"),
                arguments("  <=   AA  ", "<= AA"),

                arguments("  >   AA  ", "> AA"),
                arguments("  <   ZZ  ", "< ZZ"),
                arguments("  AA+  ", ">= AA"),

                arguments("  >=  AA   <=  ZZ  ", "[AA..ZZ]"),
                arguments("  <=  ZZ   >=  AA  ", "[AA..ZZ]"),

                arguments("  >=  AA   <  ZZ  ", "[AA..ZZ)"),
                arguments("  <  ZZ   >=  AA  ", "[AA..ZZ)"),

                arguments("  >  AA   <=  ZZ  ", "(AA..ZZ]"),
                arguments("  <=  ZZ   >  AA  ", "(AA..ZZ]"),

                arguments("  >  AA   <  ZZ  ", "(AA..ZZ)"),
                arguments("  <  ZZ   >  AA  ", "(AA..ZZ)"),

                //Part 3
                arguments("  A  A  -  Z  Z  ", "[A  A..Z  Z]"),
                arguments("  A  A  ..  Z  Z  ", "[A  A..Z  Z]"),
                arguments("  A  A   …   Z  Z  ", "(A  A..Z  Z)"),
                arguments("  A  A   ...   Z  Z  ", "(A  A..Z  Z)"),

                arguments("  [A  A  ;   Z  Z  ]  ", "[A  A..Z  Z]"),
                arguments("  (A  A  ;   Z  Z  ]  ", "(A  A..Z  Z]"),
                arguments("  [A  A  ;   Z  Z  )  ", "[A  A..Z  Z)"),
                arguments("  (A  A  ;   Z  Z  )  ", "(A  A..Z  Z)"),

                arguments("  (  A  A   ..   Z  Z  )  ", "(A  A..Z  Z)"),
                arguments("  [  A  A   ..   Z  Z  ]  ", "[A  A..Z  Z]"),
                arguments("  (  A  A   ..   Z  Z  ]  ", "(A  A..Z  Z]"),
                arguments("  [  A  A   ..   Z  Z  )  ", "[A  A..Z  Z)"),

                arguments("  A  A   and   more  ", ">= A  A"),
                arguments("  A  A   or   less  ", "<= A  A"),

                arguments("  more   than   A  A  ", "> A  A"),
                arguments("  less   than   Z  Z  ", "< Z  Z"),

                arguments("  >=   A  A  ", ">= A  A"),
                arguments("  <=   A  A  ", "<= A  A"),

                arguments("  >   A  A  ", "> A  A"),
                arguments("  <   Z  Z  ", "< Z  Z"),
                arguments("  A  A+  ", ">= A  A"),

                arguments("  >=  A  A   <=  Z  Z  ", "[A  A..Z  Z]"),
                arguments("  <=  Z  Z   >=  A  A  ", "[A  A..Z  Z]"),

                arguments("  >=  A  A   <  Z  Z  ", "[A  A..Z  Z)"),
                arguments("  <  Z  Z   >=  A  A  ", "[A  A..Z  Z)"),

                arguments("  >  A  A   <=  Z  Z  ", "(A  A..Z  Z]"),
                arguments("  <=  Z  Z   >  A  A  ", "(A  A..Z  Z]"),

                arguments("  >  A  A   <  Z  Z  ", "(A  A..Z  Z)"),
                arguments("  <  Z  Z   >  A  A  ", "(A  A..Z  Z)"));
    }

    @ParameterizedTest
    @MethodSource
    void testToStringWhitespaces(String range, String expected) {
        assertEquals(expected, new StringRange(range).toString());
    }

    @Test
    void testDoubleDash() {
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
    void testSpecialCases() {
        assertEquals("[.aaa....bbb.]", new StringRange(".aaa.-.bbb.").toString());
        assertEquals("[\\aaa\\..\\bbb\\]", new StringRange("\\aaa\\-\\bbb\\").toString());
        assertEquals("""
                        [a not so long string with the spaces in the middle and with-the-dashes-in-the-long-words\
                         becomes truncated when it defines in the String range..should work correctly]""",
                new StringRange("""
                        a not so long string with the spaces in the middle and with-the-dashes-in-the-long-words\
                         becomes truncated when it defines in the String range - should work correctly""").toString());

    }

    @Test
    void testSimpleRangeFormat() {
        var range = new StringRange("B");
        assertInclude(range, "B");
        assertExclude(range, "b", "A", "C");

        range = new StringRange("BBB");
        assertInclude(range, "BBB");
        assertExclude(range, "BB", "BBBB", "BBC", "BBA");
    }

    @Test
    void testMinMaxRangeFormat() {
        var range = new StringRange("AA-ZZ");
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
    void testBracketsFormat() {
        var range = new StringRange("[AA; ZZ]");
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
    void testVerbal() {
        var range = new StringRange("AA and more");
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
    void testMoreLessFormat() {
        var range = new StringRange(">= AA");
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
    void testMoreLessFormatBothBounds() {
        var range = new StringRange(">=AA <=ZZ");
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
    void testNulls() {
        var range = new StringRange(">=AA <=ZZ");
        assertFalse(range.contains((Range<CharSequence>) null));
        assertFalse(range.contains((CharSequence) null));
    }

    @Test
    void testNegative() {
        var range = new StringRange("00F-00Z");
        assertFalse(range.contains("Z"));
    }

    @Test
    void testParseException() {
        assertThrows(RuntimeException.class, () -> {
            new StringRange(null);
        });
    }

    private void assertInclude(StringRange range, String... args) {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertTrue(range.contains(s),
                    "The range %s must include a string '%s'".formatted(range.toString(), s));
        }
    }

    private void assertExclude(StringRange range, String... args) {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertFalse(range.contains(s),
                    "The range %s must not include a string '%s'".formatted(range.toString(), s));
        }
    }

}
